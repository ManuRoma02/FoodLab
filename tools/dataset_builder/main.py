"""FoodLab Italian Food & Nutrition Dataset Pipeline Orchestrator.

Orchestrates multi-source extraction (CREA / BDA-IEO, Open Food Facts, Scrapling GDO),
applies strict mandatory macronutrient validation, performs source-precedence deduplication,
compiles an optimized SQLite database with FTS5 search indexing, and generates
a comprehensive quality and Open Food Facts comparison report.
"""

from __future__ import annotations

import argparse
import logging
from pathlib import Path
import sqlite3
import sys
import time
from typing import Any, Dict, List, Optional, Sequence, Tuple, Union

from tools.dataset_builder.compiler import DatasetCompiler
from tools.dataset_builder.extract_chains import extract_chains_foods
from tools.dataset_builder.extract_crea import extract_crea_foods
from tools.dataset_builder.extract_off import stream_off_italian_products
from tools.dataset_builder.extract_scientific import extract_scientific_foods
from tools.dataset_builder.models import FoodRecord, PortionRecord
from tools.dataset_builder.scraper_scrapling import scrape_supermarket_catalog

logger = logging.getLogger("dataset_builder")


def setup_logging(verbose: bool = False) -> None:
    """Configure console logging level and formatting."""
    level = logging.DEBUG if verbose else logging.INFO
    formatter = logging.Formatter(
        "[%(asctime)s] %(levelname)-7s %(message)s",
        datefmt="%H:%M:%S",
    )
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)
    root_logger = logging.getLogger()
    root_logger.setLevel(level)
    # Remove existing handlers to avoid duplicates
    root_logger.handlers = [handler]


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    """Parse and validate command line arguments."""
    parser = argparse.ArgumentParser(
        prog="python -m tools.dataset_builder.main",
        description="FoodLab Italian Food & Nutrition Dataset Builder CLI",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )

    parser.add_argument(
        "--sources",
        type=str,
        default="crea,chains,off,scrapling,scientific",
        help="Comma-separated list of sources to run (choices: crea, chains, off, scrapling, scientific)",
    )
    parser.add_argument(
        "--crea-path",
        type=str,
        default="app/src/main/assets/data/crea_foods.json",
        help="Path to CREA / BDA-IEO JSON data file",
    )
    parser.add_argument(
        "--chains-path",
        type=str,
        default=None,
        help="Path to optional custom Italian fast-food / chains JSON file",
    )
    parser.add_argument(
        "--scientific-path",
        type=str,
        default=None,
        help="Path to CIQUAL or USDA CSV/JSON scientific composition file",
    )
    parser.add_argument(
        "--scientific-format",
        type=str,
        default="ciqual",
        choices=["ciqual", "usda"],
        help="Format type for scientific dataset (ciqual or usda)",
    )
    parser.add_argument(
        "--off-path",
        type=str,
        default=None,
        help="Path or URL to Open Food Facts dump (.jsonl, .jsonl.gz, .json)",
    )
    parser.add_argument(
        "--scrapling-target",
        type=str,
        default=None,
        help="Path to local HTML/JSON directory or starting URL for supermarket scraping",
    )
    parser.add_argument(
        "--output-db",
        type=str,
        default="foodlab_dataset.db",
        help="Output SQLite database destination path",
    )
    parser.add_argument(
        "--output-parquet",
        type=str,
        default=None,
        help="Optional destination path for Apache Parquet dataset export",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Maximum records to ingest per source (useful for quick builds or testing)",
    )
    parser.add_argument(
        "-v",
        "--verbose",
        action="store_true",
        help="Enable detailed debug logging",
    )

    return parser.parse_args(argv)


def run_pipeline(
    sources: Sequence[str] = ("crea", "chains", "off", "scrapling", "scientific"),
    crea_path: Optional[str] = "app/src/main/assets/data/crea_foods.json",
    chains_path: Optional[str] = None,
    scientific_path: Optional[str] = None,
    scientific_format: str = "ciqual",
    off_path: Optional[str] = None,
    scrapling_target: Optional[str] = None,
    output_db: str = "foodlab_dataset.db",
    output_parquet: Optional[str] = None,
    limit: Optional[int] = None,
) -> Dict[str, Any]:
    """
    Execute the dataset building pipeline with the specified sources and parameters.

    Returns a dictionary of execution metrics and comparison statistics.
    """
    start_time = time.time()
    active_sources = [s.strip().lower() for s in sources if s.strip()]
    valid_choices = {"crea", "chains", "off", "scrapling", "scientific"}
    invalid = [s for s in active_sources if s not in valid_choices]
    if invalid:
        raise ValueError(f"Unknown sources specified: {invalid}. Supported choices: {sorted(valid_choices)}")

    logger.info("Starting FoodLab Dataset Builder Pipeline...")
    logger.info("Active sources: %s", ", ".join(active_sources))
    logger.info("Output Database: %s", output_db)
    if output_parquet:
        logger.info("Output Parquet:  %s", output_parquet)
    if limit:
        logger.info("Per-source record limit: %d", limit)

    all_foods: List[FoodRecord] = []
    all_portions: List[PortionRecord] = []

    stats: Dict[str, Any] = {
        "sources": active_sources,
        "crea": {"extracted_foods": 0, "extracted_portions": 0},
        "chains": {"extracted_foods": 0, "extracted_portions": 0},
        "scientific": {"extracted_foods": 0},
        "off": {
            "total_evaluated": 0,
            "italian_candidates": 0,
            "rejected_non_italian": 0,
            "rejected_macros": 0,
            "valid_records": 0,
        },
        "scrapling": {"extracted_foods": 0},
        "compiled": {
            "total_input_foods": 0,
            "total_input_portions": 0,
            "inserted_foods": 0,
            "inserted_portions": 0,
            "barcoded_foods": 0,
            "unbarcoded_foods": 0,
            "parquet_rows": 0,
        },
    }

    # 1. Ingest CREA / BDA-IEO
    if "crea" in active_sources:
        if crea_path and Path(crea_path).exists():
            logger.info("Ingesting CREA data from: %s", crea_path)
            try:
                crea_foods, crea_portions = extract_crea_foods(crea_path)
                if limit is not None and limit > 0:
                    crea_foods = crea_foods[:limit]
                    # Filter portions for retained foods
                    retained_names = {f.name for f in crea_foods}
                    retained_barcodes = {f.barcode for f in crea_foods if f.barcode}
                    crea_portions = [
                        p
                        for p in crea_portions
                        if p.food_barcode_or_key in retained_names
                        or p.food_barcode_or_key in retained_barcodes
                    ]

                stats["crea"]["extracted_foods"] = len(crea_foods)
                stats["crea"]["extracted_portions"] = len(crea_portions)
                all_foods.extend(crea_foods)
                all_portions.extend(crea_portions)
                logger.info(
                    "CREA: Extracted %d foods and %d portions",
                    len(crea_foods),
                    len(crea_portions),
                )
            except Exception as e:
                logger.error("Failed to extract CREA data: %s", e)
        else:
            logger.warning("CREA path not found: %s. Skipping CREA.", crea_path)

    # 2. Ingest Italian Chains & Fast Food
    if "chains" in active_sources:
        logger.info("Ingesting Italian Fast Food & Restaurant Chains...")
        try:
            chain_foods, chain_portions = extract_chains_foods(custom_json_path=chains_path, limit=limit)
            stats["chains"]["extracted_foods"] = len(chain_foods)
            stats["chains"]["extracted_portions"] = len(chain_portions)
            all_foods.extend(chain_foods)
            all_portions.extend(chain_portions)
            logger.info(
                "Chains: Extracted %d foods and %d portions",
                len(chain_foods),
                len(chain_portions),
            )
        except Exception as e:
            logger.error("Failed to extract Chains data: %s", e)

    # 3. Ingest Scientific Databases (CIQUAL / USDA)
    if "scientific" in active_sources:
        logger.info("Ingesting Scientific databases (CIQUAL / USDA)...")
        try:
            sci_foods = extract_scientific_foods(
                file_path=scientific_path,
                format_type=scientific_format,
                limit=limit,
            )
            stats["scientific"]["extracted_foods"] = len(sci_foods)
            all_foods.extend(sci_foods)
            logger.info("Scientific: Extracted %d foods", len(sci_foods))
        except Exception as e:
            logger.error("Failed to extract Scientific data: %s", e)

    # 4. Ingest Open Food Facts
    if "off" in active_sources:
        if off_path:
            logger.info("Ingesting Open Food Facts data from: %s", off_path)
            off_stats = stats["off"]
            off_foods: List[FoodRecord] = []
            try:
                for record in stream_off_italian_products(
                    off_path,
                    limit=limit,
                    filter_italian=True,
                    stats=off_stats,
                ):
                    off_foods.append(record)
                    all_foods.append(record)

                logger.info(
                    "Open Food Facts: Streamed %d valid foods (Evaluated: %d, Rejected by Macro Gate: %d)",
                    len(off_foods),
                    off_stats["total_evaluated"],
                    off_stats["rejected_macros"],
                )
            except Exception as e:
                logger.error("Failed to extract Open Food Facts data: %s", e)
        else:
            logger.info("No --off-path specified. Skipping Open Food Facts ingestion.")

    # 3. Ingest Scrapling Supermarket Catalog
    if "scrapling" in active_sources:
        if scrapling_target:
            logger.info("Ingesting Supermarket data from: %s", scrapling_target)
            try:
                scrapling_foods = scrape_supermarket_catalog(
                    scrapling_target,
                    limit=limit or 500,
                )
                stats["scrapling"]["extracted_foods"] = len(scrapling_foods)
                all_foods.extend(scrapling_foods)
                logger.info("Scrapling: Extracted %d foods", len(scrapling_foods))
            except Exception as e:
                logger.error("Failed to scrape supermarket data: %s", e)
        else:
            logger.info("No --scrapling-target specified. Skipping Supermarket scraping.")

    stats["compiled"]["total_input_foods"] = len(all_foods)
    stats["compiled"]["total_input_portions"] = len(all_portions)

    # 4. Master Compilation, Deduplication & Indexing
    logger.info("Compiling and deduplicating %d food records into %s...", len(all_foods), output_db)
    compiler = DatasetCompiler(output_db)
    inserted_foods, inserted_portions = compiler.compile(all_foods, all_portions)
    stats["compiled"]["inserted_foods"] = inserted_foods
    stats["compiled"]["inserted_portions"] = inserted_portions

    # Query DB for barcoded vs unbarcoded distribution
    conn = sqlite3.connect(output_db)
    try:
        cur = conn.cursor()
        cur.execute("SELECT COUNT(*) FROM foods WHERE barcode IS NOT NULL AND barcode != ''")
        stats["compiled"]["barcoded_foods"] = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM foods WHERE barcode IS NULL OR barcode = ''")
        stats["compiled"]["unbarcoded_foods"] = cur.fetchone()[0]
    finally:
        conn.close()

    # 5. Export Parquet if requested
    if output_parquet:
        logger.info("Exporting dataset to Apache Parquet: %s", output_parquet)
        exported_rows = compiler.export_parquet(output_parquet)
        stats["compiled"]["parquet_rows"] = exported_rows
        logger.info("Parquet export complete: %d rows written.", exported_rows)

    elapsed = time.time() - start_time
    stats["elapsed_seconds"] = round(elapsed, 2)

    logger.info("Dataset compilation complete in %.2f seconds.", elapsed)

    # 6. Print Comprehensive Statistics & Open Food Facts Comparison Report
    print_comparison_report(stats)

    return stats


def print_comparison_report(stats: Dict[str, Any]) -> None:
    """Print the detailed quality report and Open Food Facts vs FoodLab comparison."""
    off_stats = stats["off"]
    crea_stats = stats["crea"]
    scrapling_stats = stats["scrapling"]
    compiled = stats["compiled"]

    report = []
    sep = "=" * 74
    subsep = "-" * 74

    report.append("")
    report.append(sep)
    report.append("     STATISTICS & COMPARISON REPORT: FOODLAB vs OPEN FOOD FACTS")
    report.append(sep)
    report.append("")

    # Section 1: Extraction Summary
    report.append("[1] INGESTION & PIPELINE COUNTERS")
    report.append(subsep)
    report.append(f"  * CREA / BDA-IEO Foods Ingested:       {crea_stats['extracted_foods']:>8,d}")
    report.append(f"  * CREA Linked Portions Extracted:      {crea_stats['extracted_portions']:>8,d}")
    report.append(f"  * Scrapling Supermarket Foods:         {scrapling_stats['extracted_foods']:>8,d}")
    report.append(f"  * Open Food Facts Raw Evaluated:       {off_stats['total_evaluated']:>8,d}")
    if off_stats["total_evaluated"] > 0:
        report.append(f"    - Italian Candidates:                {off_stats['italian_candidates']:>8,d}")
        report.append(f"    - Excluded (Non-Italian Market):     {off_stats['rejected_non_italian']:>8,d}")
        report.append(f"    - Rejected by Mandatory Macro Gate:  {off_stats['rejected_macros']:>8,d}")
        report.append(f"    - Clean Valid OFF Foods Extracted:   {off_stats['valid_records']:>8,d}")
    report.append("")

    # Section 2: Final Database Summary
    report.append("[2] FINAL FOODLAB DATABASE SUMMARY")
    report.append(subsep)
    report.append(f"  * Total Raw Foods Submitted:           {compiled['total_input_foods']:>8,d}")
    report.append(f"  * Total Clean Unique Foods (SQLite):   {compiled['inserted_foods']:>8,d}")
    report.append(f"    - Barcoded Products:                 {compiled['barcoded_foods']:>8,d}")
    report.append(f"    - Unbarcoded / Generic Whole Foods:  {compiled['unbarcoded_foods']:>8,d}")
    report.append(f"  * Total Food Portions Linked:          {compiled['inserted_portions']:>8,d}")
    if compiled.get("parquet_rows", 0) > 0:
        report.append(f"  * Exported Parquet Records:            {compiled['parquet_rows']:>8,d}")
    report.append("")

    # Section 3: Open Food Facts vs FoodLab Quality & Quantity Comparison
    report.append("[3] RESOCONTO COMPARATIVO CON OPEN FOOD FACTS")
    report.append(subsep)
    report.append("  Perché FoodLab differisce profondamente dal catalogo grezzo di Open Food Facts:")
    report.append("")
    report.append("  1. MANDATORY MACRO GATE (Garanzia Qualità):")
    report.append("     Open Food Facts accetta contributi 'crowdsourced' privi di macronutrienti")
    report.append("     obbligatori o con valori fisicamente impossibili (es. grassi + carbo + prot > 100g).")
    if off_stats["total_evaluated"] > 0:
        pct_rej = (
            (off_stats["rejected_macros"] / off_stats["italian_candidates"] * 100)
            if off_stats["italian_candidates"] > 0
            else 0.0
        )
        report.append(f"     Su {off_stats['italian_candidates']} prodotti italiani esaminati, ben {off_stats['rejected_macros']} ({pct_rej:.1f}%)")
        report.append("     sono stati SCARTATI perché privi dei 4 macro essenziali o scientificamente invalidi.")
    else:
        report.append("     Tutti i record senza calorie, proteine, carboidrati o grassi vengono scartati.")
    report.append("")
    report.append("  2. GERARCHIA DELLE FONTI (Source Precedence Hierarchy):")
    report.append("     CREA (3) > SCRAPLING GDO (2) > OPEN FOOD FACTS (1)")
    report.append("     Se un alimento (es. Olio Extravergine, Pasta o Petto di Pollo) esiste sia")
    report.append("     in CREA che in Open Food Facts, FoodLab privilegia sempre la composizione")
    report.append("     nutrizionale verificata di CREA, arricchendo i metadati mancanti senza corrompere i macro.")
    report.append("")
    report.append("  3. CONFRONTO DIRETTO DEI NUMERI:")
    report.append("     +----------------------------------+------------------+------------------+")
    report.append("     | Metrica                          | Open Food Facts  | FoodLab Database |")
    report.append("     +----------------------------------+------------------+------------------+")
    report.append(f"     | Alimenti Grezzi / Totali         | {off_stats['total_evaluated']:>16,d} | {compiled['total_input_foods']:>16,d} |")
    report.append(f"     | Macro Incompleti / Scartati      | {off_stats['rejected_macros']:>16,d} |                0 |")
    report.append(f"     | Alimenti Validi al 100% Macro    | {off_stats['valid_records']:>16,d} | {compiled['inserted_foods']:>16,d} |")
    report.append(f"     | Prodotti con Barcode             | {off_stats['valid_records']:>16,d} | {compiled['barcoded_foods']:>16,d} |")
    report.append(f"     | Cibi Generici / Senza Barcode    |                0 | {compiled['unbarcoded_foods']:>16,d} |")
    report.append(f"     | Porzioni Standardizzate Collegate|                0 | {compiled['inserted_portions']:>16,d} |")
    report.append("     +----------------------------------+------------------+------------------+")
    report.append("")
    report.append(sep)
    report.append("")

    print("\n".join(report))


def main(argv: Optional[Sequence[str]] = None) -> int:
    """CLI main entry point."""
    args = parse_args(argv)
    setup_logging(args.verbose)

    source_list = [s.strip() for s in args.sources.split(",") if s.strip()]

    try:
        run_pipeline(
            sources=source_list,
            crea_path=args.crea_path,
            chains_path=args.chains_path,
            scientific_path=args.scientific_path,
            scientific_format=args.scientific_format,
            off_path=args.off_path,
            scrapling_target=args.scrapling_target,
            output_db=args.output_db,
            output_parquet=args.output_parquet,
            limit=args.limit,
        )
        return 0
    except Exception as e:
        logger.exception("Pipeline failed: %s", e)
        return 1


if __name__ == "__main__":
    sys.exit(main())
