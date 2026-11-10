"""Open Food Facts Italy streaming ingestion module for FoodLab.

Streams, filters, and normalizes Italian food products from Open Food Facts
exports (.jsonl.gz, .jsonl, .json, or HTTP/HTTPS stream) into verified FoodRecords.
"""

from __future__ import annotations

import gzip
import io
import json
from pathlib import Path
from typing import Any, Dict, Iterator, Optional, Union
import urllib.request

from tools.dataset_builder.models import FoodRecord
from tools.dataset_builder.validator import validate_and_normalize


def is_italian_product(raw: Dict[str, Any]) -> bool:
    """Determine whether a raw Open Food Facts record targets the Italian market.

    Checks country tags, country hierarchy, localized strings, and language indicators.
    """
    if not isinstance(raw, dict):
        return False

    # 1. Check countries_tags
    countries_tags = raw.get("countries_tags")
    if isinstance(countries_tags, list):
        for tag in countries_tags:
            tag_str = str(tag).strip().lower()
            if (
                tag_str in ("en:italy", "it", "it:italia", "en:it")
                or "italy" in tag_str
                or tag_str.endswith(":italy")
            ):
                return True

    # 2. Check countries hierarchy
    countries_hierarchy = raw.get("countries_hierarchy")
    if isinstance(countries_hierarchy, list):
        for tag in countries_hierarchy:
            tag_str = str(tag).strip().lower()
            if "italy" in tag_str or tag_str in ("en:italy", "it", "it:italia"):
                return True

    # 3. Check raw countries string
    countries = raw.get("countries")
    if isinstance(countries, str):
        c_lower = countries.strip().lower()
        if "italia" in c_lower or "italy" in c_lower or c_lower == "it":
            return True

    # 4. Check languages_tags / languages_hierarchy
    for key in ("languages_tags", "languages_hierarchy"):
        langs = raw.get(key)
        if isinstance(langs, list):
            for lang_tag in langs:
                lt_str = str(lang_tag).strip().lower()
                if (
                    lt_str in ("it", "en:italian", "en:it", "it:italian")
                    or "italian" in lt_str
                ):
                    return True

    # 5. Check primary lang code
    lang = raw.get("lang")
    if isinstance(lang, str) and lang.strip().lower() in ("it", "ita"):
        return True

    # 6. Check languages mapping or list
    languages = raw.get("languages")
    if isinstance(languages, dict):
        if any(k.strip().lower() in ("it", "ita", "en:it") for k in languages):
            return True
    elif isinstance(languages, list):
        if any(str(l).strip().lower() in ("it", "ita", "en:it") for l in languages):
            return True

    return False


def parse_off_product_json(
    raw: Dict[str, Any],
    filter_italian: bool = True,
) -> Optional[FoodRecord]:
    """Parse a single Open Food Facts raw product dictionary into a validated FoodRecord.

    Returns None if:
    - raw is not a dictionary.
    - filter_italian is True and product does not belong to Italian market.
    - mandatory macronutrients (calories, protein, carbs, fat) are missing or invalid.
    - physical consistency checks (sum > 102g, Atwater factor sanity) fail.
    """
    if not isinstance(raw, dict):
        return None

    if filter_italian and not is_italian_product(raw):
        return None

    adapted = dict(raw)

    # 1. Product name resolution: prefer Italian localized name if present
    name = (
        raw.get("product_name_it")
        or raw.get("product_name")
        or raw.get("generic_name_it")
        or raw.get("generic_name")
        or raw.get("name")
    )
    if not name or not str(name).strip():
        return None
    adapted["name"] = str(name).strip()

    # 2. Category resolution: if categories string is absent, infer from categories_tags
    if not adapted.get("categories") and not adapted.get("category"):
        cat_tags = raw.get("categories_tags")
        if isinstance(cat_tags, list) and cat_tags:
            cleaned_tags = [
                str(t).split(":")[-1].replace("-", " ").capitalize()
                for t in cat_tags
                if isinstance(t, str) and t.strip()
            ]
            if cleaned_tags:
                adapted["category"] = ", ".join(cleaned_tags[:3])

    # 3. Serving size fallback from serving_quantity if serving_size string missing
    if not adapted.get("serving_size") and not adapted.get("default_serving_size"):
        serving_qty = raw.get("serving_quantity")
        if serving_qty is not None:
            adapted["default_serving_size"] = serving_qty

    # 4. Energy fallback: convert kJ to kcal if energy-kcal_100g is missing
    nutriments = raw.get("nutriments")
    if isinstance(nutriments, dict):
        has_kcal = any(
            nutriments.get(k) is not None
            for k in ("energy-kcal_100g", "energy-kcal", "energy_kcal")
        )
        if not has_kcal:
            energy_val = nutriments.get("energy_100g")
            if energy_val is None:
                energy_val = nutriments.get("energy")
            energy_unit = str(nutriments.get("energy_unit") or "").strip().lower()
            if energy_val is not None:
                try:
                    val = float(energy_val)
                    if energy_unit == "kcal":
                        adapted_nutriments = dict(nutriments)
                        adapted_nutriments["energy-kcal_100g"] = val
                        adapted["nutriments"] = adapted_nutriments
                    elif energy_unit in ("kj", "joule", "joules") or val > 950:
                        adapted_nutriments = dict(nutriments)
                        adapted_nutriments["energy-kcal_100g"] = round(val / 4.184)
                        adapted["nutriments"] = adapted_nutriments
                except (ValueError, TypeError):
                    pass

    return validate_and_normalize(adapted, source="openfoodfacts")


def _open_stream(file_or_url: Union[str, Path]) -> tuple[Iterator[str], Any]:
    """Open a line stream from either a local file or HTTP/HTTPS URL.

    Supports .jsonl, .jsonl.gz, .ndjson, and plain .json.
    Returns a tuple of (line_iterator, context_manager_or_none).
    """
    url_str = str(file_or_url)

    # 1. Remote HTTP/HTTPS stream
    if url_str.startswith(("http://", "https://")):
        req = urllib.request.Request(
            url_str,
            headers={"User-Agent": "FoodLab-DatasetBuilder/1.0"},
        )
        resp = urllib.request.urlopen(req, timeout=30)
        is_gzip = url_str.endswith(".gz") or resp.headers.get("Content-Encoding") == "gzip"
        if is_gzip:
            gz_stream = gzip.GzipFile(fileobj=resp)
            text_stream = io.TextIOWrapper(gz_stream, encoding="utf-8", errors="replace")
            return text_stream, resp
        else:
            text_stream = io.TextIOWrapper(resp, encoding="utf-8", errors="replace")
            return text_stream, resp

    # 2. Local File
    path = Path(file_or_url)
    if not path.is_file():
        raise FileNotFoundError(f"Open Food Facts source file not found: {file_or_url}")

    if path.name.endswith(".gz"):
        f = gzip.open(path, mode="rt", encoding="utf-8", errors="replace")
        return f, f
    else:
        f = open(path, mode="r", encoding="utf-8", errors="replace")
        return f, f


def stream_off_italian_products(
    file_or_url: Union[str, Path],
    limit: Optional[int] = None,
    filter_italian: bool = True,
    stats: Optional[Dict[str, int]] = None,
) -> Iterator[FoodRecord]:
    """Stream and yield validated FoodRecords for Italian products from Open Food Facts.

    Processes data line-by-line without loading entire files into memory.
    Supports .jsonl, .jsonl.gz, .ndjson, .json arrays, and remote URLs.

    Args:
        file_or_url: Local file path or URL to OFF dump.
        limit: Maximum number of valid FoodRecord items to yield.
        filter_italian: Whether to filter strictly for Italian products.
        stats: Optional dictionary to collect streaming ingestion metrics.
    """
    if limit is not None and limit <= 0:
        return

    # Check if local file is a standard JSON file (array or object)
    url_str = str(file_or_url)
    is_url = url_str.startswith(("http://", "https://"))

    if not is_url:
        path = Path(file_or_url)
        if not path.is_file():
            raise FileNotFoundError(f"Open Food Facts source file not found: {file_or_url}")

        # Check for standard JSON format (non-gzip, ending with .json)
        if path.suffix.lower() == ".json" and not path.name.endswith(".jsonl"):
            # Peek first non-whitespace character
            with open(path, "r", encoding="utf-8", errors="replace") as f:
                first_char = ""
                for char in f.read(1024):
                    if not char.isspace():
                        first_char = char
                        break

            # If it starts with '[', it's a JSON array
            if first_char == "[":
                with open(path, "r", encoding="utf-8", errors="replace") as f:
                    data = json.load(f)
                if isinstance(data, list):
                    yielded = 0
                    for raw_item in data:
                        if isinstance(raw_item, dict):
                            if stats is not None:
                                stats["total_evaluated"] = stats.get("total_evaluated", 0) + 1
                            is_it = is_italian_product(raw_item)
                            if filter_italian and not is_it:
                                if stats is not None:
                                    stats["rejected_non_italian"] = stats.get("rejected_non_italian", 0) + 1
                                continue
                            if stats is not None:
                                stats["italian_candidates"] = stats.get("italian_candidates", 0) + 1

                            record = parse_off_product_json(raw_item, filter_italian=filter_italian)
                            if record is not None:
                                if stats is not None:
                                    stats["valid_records"] = stats.get("valid_records", 0) + 1
                                yield record
                                yielded += 1
                                if limit is not None and yielded >= limit:
                                    return
                            else:
                                if stats is not None:
                                    stats["rejected_macros"] = stats.get("rejected_macros", 0) + 1
                    return

    # Streaming mode for line-delimited JSON (JSONL / JSONL.GZ / streams)
    stream, closer = _open_stream(file_or_url)
    yielded = 0

    try:
        for line in stream:
            line_str = line.strip()
            if not line_str:
                continue

            try:
                raw_item = json.loads(line_str)
            except (json.JSONDecodeError, ValueError):
                continue

            if not isinstance(raw_item, dict):
                continue

            if stats is not None:
                stats["total_evaluated"] = stats.get("total_evaluated", 0) + 1

            is_it = is_italian_product(raw_item)
            if filter_italian and not is_it:
                if stats is not None:
                    stats["rejected_non_italian"] = stats.get("rejected_non_italian", 0) + 1
                continue
            if stats is not None:
                stats["italian_candidates"] = stats.get("italian_candidates", 0) + 1

            record = parse_off_product_json(raw_item, filter_italian=filter_italian)
            if record is not None:
                if stats is not None:
                    stats["valid_records"] = stats.get("valid_records", 0) + 1
                yield record
                yielded += 1
                if limit is not None and yielded >= limit:
                    break
            else:
                if stats is not None:
                    stats["rejected_macros"] = stats.get("rejected_macros", 0) + 1
    finally:
        if closer is not None and hasattr(closer, "close"):
            closer.close()
