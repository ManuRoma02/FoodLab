"""Scrapling Italian Supermarket (GDO) Crawler and Nutrition Label Parser.

Extracts, normalizes, and validates Italian food product data from supermarket catalogs,
raw HTML product pages, JSON-LD Schema.org metadata, and nutrition tables.
"""

from __future__ import annotations

import html
import json
import logging
from pathlib import Path
import re
from typing import Any, Callable, Dict, List, Optional, Set, Union
import urllib.parse
import urllib.request

import requests

from tools.dataset_builder.models import FoodRecord
from tools.dataset_builder.validator import _parse_numeric, validate_and_normalize

logger = logging.getLogger(__name__)

# Scrapling / BS4 optional imports with graceful fallback
try:
    from scrapling.parser import Selector as ScraplingSelector
    Adaptor = ScraplingSelector
except ImportError:
    try:
        from scrapling import Adaptor  # type: ignore
        ScraplingSelector = Adaptor
    except ImportError:
        ScraplingSelector = None
        Adaptor = None

try:
    from scrapling import Fetcher, StealthyFetcher  # type: ignore
    HAS_SCRAPLING_FETCHERS = True
except (ImportError, Exception):
    Fetcher = None
    StealthyFetcher = None
    HAS_SCRAPLING_FETCHERS = False

try:
    from bs4 import BeautifulSoup
except ImportError:
    BeautifulSoup = None


# Italian nutrition label key dictionary mapping to canonical FoodRecord fields
NUTRITION_FIELD_MAP: Dict[str, str] = {
    # Calories / Energy
    "energia": "calories_100g",
    "valore energetico": "calories_100g",
    "energy": "calories_100g",
    "calorie": "calories_100g",
    "valore energetico (kcal)": "calories_100g",
    "energia (kcal)": "calories_100g",
    # Fat
    "grassi": "fat_100g",
    "grassi totali": "fat_100g",
    "lipidi": "fat_100g",
    "lipidi totali": "fat_100g",
    "fat": "fat_100g",
    "materia grassa": "fat_100g",
    # Saturated fat
    "di cui acidi grassi saturi": "saturated_fat_100g",
    "di cui grassi saturi": "saturated_fat_100g",
    "acidi grassi saturi": "saturated_fat_100g",
    "grassi saturi": "saturated_fat_100g",
    "saturi": "saturated_fat_100g",
    "acidi grassi saturi (g)": "saturated_fat_100g",
    "saturated fat": "saturated_fat_100g",
    # Carbs
    "carboidrati": "carbs_100g",
    "carboidrati totali": "carbs_100g",
    "carbohydrates": "carbs_100g",
    "glucidi": "carbs_100g",
    # Sugars
    "di cui zuccheri": "sugars_100g",
    "zuccheri": "sugars_100g",
    "di cui glucidi": "sugars_100g",
    "sugars": "sugars_100g",
    # Polyols
    "di cui polioli": "polyols_100g",
    "di cui polialcoli": "polyols_100g",
    "polioli": "polyols_100g",
    "polialcoli": "polyols_100g",
    "polyols": "polyols_100g",
    # Fiber
    "fibre": "fiber_100g",
    "fibre alimentari": "fiber_100g",
    "fibra": "fiber_100g",
    "fibra alimentare": "fiber_100g",
    "fiber": "fiber_100g",
    "fibres": "fiber_100g",
    "dietary fiber": "fiber_100g",
    # Protein
    "proteine": "protein_100g",
    "proteine totali": "protein_100g",
    "protein": "protein_100g",
    "proteins": "protein_100g",
    # Salt
    "sale": "salt_100g",
    "sale (g)": "salt_100g",
    "salt": "salt_100g",
    # Sodium
    "sodio": "sodium_100g",
    "sodium": "sodium_100g",
}


def clean_label_key(raw_key: str) -> str:
    """Normalize label key by stripping leading bullets, dashes, dots, and unit qualifiers."""
    text = str(raw_key).strip().lower()
    text = text.replace("\xa0", " ")
    # Strip leading bullets, dashes, asterisks, dots
    text = re.sub(r"^[-*•·–—\.\s]+", "", text)
    # Strip trailing colon or punctuation
    text = re.sub(r"[:\s]+$", "", text)
    # Collapse consecutive whitespace
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def parse_energy_value(val: Any) -> Optional[float]:
    """
    Extract energy in kcal from an energy string or number.

    Correctly prioritizes kcal over kJ when both are present (e.g. '1985 kJ / 473 kcal'),
    and converts standalone kJ values to kcal (1 kcal = 4.184 kJ).
    """
    if val is None:
        return None
    if isinstance(val, (int, float)):
        import math
        return None if math.isnan(val) or math.isinf(val) else float(val)
    if not isinstance(val, str):
        return None

    cleaned = val.strip().lower().replace("\xa0", " ").replace(",", ".")

    # 1. Look for explicit kcal match
    kcal_match = re.search(r"(\d+(?:\.\d+)?)\s*kcal", cleaned)
    if kcal_match:
        try:
            return float(kcal_match.group(1))
        except ValueError:
            pass

    # 2. Look for kJ match when no kcal is specified
    kj_match = re.search(r"(\d+(?:\.\d+)?)\s*kj", cleaned)
    if kj_match:
        try:
            kj_num = float(kj_match.group(1))
            return kj_num / 4.184
        except ValueError:
            pass

    # 3. Fallback to general numeric parser
    return _parse_numeric(cleaned)


def extract_nutrient_from_string(val: Any) -> Optional[float]:
    """Parse numeric nutrient value handling commas, units, <, and trace indicators."""
    if val is None:
        return None
    if isinstance(val, (int, float)):
        import math
        return None if math.isnan(val) or math.isinf(val) else float(val)
    if not isinstance(val, str):
        return None

    text = val.strip().lower().replace("\xa0", " ")
    if not text or text in ("-", "--", "n/a", "na", "n.d.", "nd", "assente", "assenti"):
        return None
    if text in ("tr", "traces", "tracce", "tr.", "< tr", "<tr"):
        return 0.0

    # Handle '< 0,1' or 'inferiore a 0,1'
    if text.startswith("<"):
        text = text[1:].strip()
    elif text.startswith("inferiore a"):
        text = text[len("inferiore a"):].strip()

    is_mg = False
    if "mg" in text:
        is_mg = True
        text = text.replace("mg", "")
    elif "g" in text:
        text = text.replace("g", "")

    text = text.replace(",", ".")
    match = re.search(r"[-+]?\d*\.?\d+", text)
    if match:
        try:
            num = float(match.group(0))
            if is_mg:
                num /= 1000.0
            return num
        except ValueError:
            return None
    return None


def _extract_json_ld_products(html_text: str) -> List[Dict[str, Any]]:
    """Extract Schema.org Product objects and their NutritionInformation from HTML."""
    products: List[Dict[str, Any]] = []
    # Match all script tags with application/ld+json
    pattern = re.compile(
        r'<script[^>]*type=["\']application/ld\+json["\'][^>]*>(.*?)</script>',
        re.DOTALL | re.IGNORECASE,
    )
    for match in pattern.finditer(html_text):
        script_content = match.group(1).strip()
        if not script_content:
            continue
        try:
            data = json.loads(script_content)
        except Exception:
            continue

        items = []
        if isinstance(data, list):
            items = data
        elif isinstance(data, dict):
            if "@graph" in data and isinstance(data["@graph"], list):
                items = data["@graph"]
            else:
                items = [data]

        for item in items:
            if not isinstance(item, dict):
                continue
            item_type = str(item.get("@type", ""))
            if "Product" in item_type or item.get("nutrition") is not None:
                # Extract fields
                prod_data: Dict[str, Any] = {
                    "name": item.get("name") or "",
                    "brand": "",
                    "barcode": "",
                    "category": item.get("category") or "",
                }
                # Brand
                raw_brand = item.get("brand")
                if isinstance(raw_brand, dict):
                    prod_data["brand"] = raw_brand.get("name", "")
                elif isinstance(raw_brand, str):
                    prod_data["brand"] = raw_brand

                # Barcode / EAN
                for b_key in ("gtin13", "gtin8", "gtin", "gtin14", "ean", "sku", "productID", "barcode"):
                    if item.get(b_key):
                        prod_data["barcode"] = str(item[b_key])
                        break

                # Nutrition
                nutrition = item.get("nutrition")
                if isinstance(nutrition, dict):
                    if nutrition.get("calories"):
                        prod_data["calories_100g"] = parse_energy_value(nutrition["calories"])
                    if nutrition.get("proteinContent"):
                        prod_data["protein_100g"] = extract_nutrient_from_string(nutrition["proteinContent"])
                    if nutrition.get("carbohydrateContent"):
                        prod_data["carbs_100g"] = extract_nutrient_from_string(nutrition["carbohydrateContent"])
                    if nutrition.get("sugarContent"):
                        prod_data["sugars_100g"] = extract_nutrient_from_string(nutrition["sugarContent"])
                    if nutrition.get("fatContent"):
                        prod_data["fat_100g"] = extract_nutrient_from_string(nutrition["fatContent"])
                    if nutrition.get("saturatedFatContent"):
                        prod_data["saturated_fat_100g"] = extract_nutrient_from_string(nutrition["saturatedFatContent"])
                    if nutrition.get("fiberContent"):
                        prod_data["fiber_100g"] = extract_nutrient_from_string(nutrition["fiberContent"])
                    if nutrition.get("sodiumContent"):
                        prod_data["sodium_100g"] = extract_nutrient_from_string(nutrition["sodiumContent"])
                    if nutrition.get("servingSize"):
                        prod_data["default_serving_size"] = extract_nutrient_from_string(nutrition["servingSize"])

                if prod_data["name"] or "calories_100g" in prod_data:
                    products.append(prod_data)

    return products


def _extract_html_metadata(html_text: str) -> Dict[str, str]:
    """Extract product title, brand, and barcode from HTML elements."""
    metadata = {"name": "", "brand": "", "barcode": ""}

    if BeautifulSoup is not None:
        soup = BeautifulSoup(html_text, "html.parser")

        # 1. Title / Name
        title_el = (
            soup.select_one("h1.product-title")
            or soup.select_one("h1.product-name")
            or soup.select_one("[itemprop='name']")
            or soup.select_one("h1")
        )
        if title_el:
            metadata["name"] = title_el.get_text().strip()
        else:
            og_title = soup.select_one("meta[property='og:title']")
            if og_title and og_title.get("content"):
                metadata["name"] = str(og_title["content"]).strip()
            elif soup.title and soup.title.string:
                metadata["name"] = soup.title.string.strip()

        # 2. Brand
        brand_el = (
            soup.select_one(".product-brand")
            or soup.select_one(".brand")
            or soup.select_one("[itemprop='brand']")
        )
        if brand_el:
            metadata["brand"] = brand_el.get_text().strip()
        else:
            og_brand = soup.select_one("meta[property='og:brand']") or soup.select_one("meta[name='brand']")
            if og_brand and og_brand.get("content"):
                metadata["brand"] = str(og_brand["content"]).strip()

        # 3. Barcode
        # Check elements with barcode attributes
        barcode_el = (
            soup.select_one("[data-ean]")
            or soup.select_one("[data-barcode]")
            or soup.select_one("[data-gtin]")
            or soup.select_one("[data-code]")
        )
        if barcode_el:
            bc = (
                barcode_el.get("data-ean")
                or barcode_el.get("data-barcode")
                or barcode_el.get("data-gtin")
                or barcode_el.get("data-code")
            )
            if bc:
                metadata["barcode"] = str(bc).strip()

        if not metadata["barcode"]:
            # Search text for barcode pattern
            text_match = re.search(
                r"\b(?:ean|barcode|gtin|codice a barre|codice)[\s:#]+(\d{8,14})\b",
                soup.get_text(),
                re.IGNORECASE,
            )
            if text_match:
                metadata["barcode"] = text_match.group(1).strip()

    else:
        # Regex fallback when BeautifulSoup is unavailable
        h1_m = re.search(r"<h1[^>]*>(.*?)</h1>", html_text, re.IGNORECASE | re.DOTALL)
        if h1_m:
            metadata["name"] = re.sub(r"<[^>]+>", "", h1_m.group(1)).strip()

        brand_m = re.search(r'class=["\'][^"\']*(?:product-brand|brand)[^"\']*["\'][^>]*>(.*?)<', html_text, re.IGNORECASE)
        if brand_m:
            metadata["brand"] = brand_m.group(1).strip()

        ean_m = re.search(r'data-ean=["\'](\d+)["\']', html_text, re.IGNORECASE)
        if ean_m:
            metadata["barcode"] = ean_m.group(1).strip()
        else:
            bc_m = re.search(r"\b(?:ean|barcode|gtin)[\s:#]+(\d{8,14})\b", html_text, re.IGNORECASE)
            if bc_m:
                metadata["barcode"] = bc_m.group(1).strip()

    return metadata


def _extract_html_nutrition_table(html_text: str) -> Dict[str, str]:
    """Parse HTML table rows into a dictionary of label -> value."""
    table_dict: Dict[str, str] = {}

    if BeautifulSoup is not None:
        soup = BeautifulSoup(html_text, "html.parser")
        tables = soup.find_all("table")

        for table in tables:
            t_text = table.get_text().lower()
            if any(k in t_text for k in ("energia", "grassi", "carboidrati", "proteine", "valori nutrizionali", "nutrizion")):
                # Determine target column for 100g
                col_idx = 1
                headers = table.find_all("th")
                for idx, th in enumerate(headers):
                    th_str = th.get_text().strip().lower()
                    if "100" in th_str and ("g" in th_str or "ml" in th_str):
                        col_idx = idx
                        break

                for tr in table.find_all("tr"):
                    cells = tr.find_all(["td", "th"])
                    if len(cells) >= 2:
                        label = cells[0].get_text().strip()
                        target_cell = cells[col_idx] if col_idx < len(cells) else cells[1]
                        val = target_cell.get_text().strip()
                        clean_k = clean_label_key(label)
                        if clean_k:
                            table_dict[label] = val

                if table_dict:
                    return table_dict

        # Also check definition list <dl>
        for dl in soup.find_all("dl"):
            dl_text = dl.get_text().lower()
            if any(k in dl_text for k in ("energia", "grassi", "carboidrati", "proteine")):
                dts = dl.find_all("dt")
                dds = dl.find_all("dd")
                for dt, dd in zip(dts, dds):
                    table_dict[dt.get_text().strip()] = dd.get_text().strip()
                if table_dict:
                    return table_dict

    else:
        # Regex fallback for HTML tables
        rows = re.findall(r"<tr[^>]*>(.*?)</tr>", html_text, re.IGNORECASE | re.DOTALL)
        for r in rows:
            cells = re.findall(r"<(?:td|th)[^>]*>(.*?)</(?:td|th)>", r, re.IGNORECASE | re.DOTALL)
            if len(cells) >= 2:
                k = re.sub(r"<[^>]+>", "", cells[0]).strip()
                v = re.sub(r"<[^>]+>", "", cells[1]).strip()
                if k:
                    table_dict[k] = v

    return table_dict


def parse_supermarket_html_or_json(raw: Any, source: str = "SCRAPLING_GDO") -> Optional[FoodRecord]:
    """
    Parse a supermarket product from dict, JSON string, or HTML document.

    Handles Italian nutrition label key synonyms, Schema.org JSON-LD data,
    and returns a validated and physically normalized FoodRecord.
    """
    if raw is None:
        return None

    # 1. If string input, determine if JSON or HTML
    if isinstance(raw, str):
        stripped = raw.strip()
        if not stripped:
            return None

        # Try parsing JSON string first
        if (stripped.startswith("{") and stripped.endswith("}")) or (stripped.startswith("[") and stripped.endswith("]")):
            try:
                parsed_json = json.loads(stripped)
                if isinstance(parsed_json, dict):
                    raw = parsed_json
                elif isinstance(parsed_json, list) and len(parsed_json) > 0 and isinstance(parsed_json[0], dict):
                    raw = parsed_json[0]
            except Exception:
                pass

    # 2. If input is HTML document or fragment
    if isinstance(raw, str):
        # A. Check for Schema.org JSON-LD product data
        json_ld_products = _extract_json_ld_products(raw)
        for prod in json_ld_products:
            rec = validate_and_normalize(prod, source=source)
            if rec is not None:
                return rec

        # B. Parse HTML DOM table and metadata
        metadata = _extract_html_metadata(raw)
        html_table = _extract_html_nutrition_table(raw)

        if not html_table and not metadata.get("name"):
            return None

        raw = {
            **metadata,
            "nutrition_table": html_table,
        }

    # 3. Process dictionary
    if not isinstance(raw, dict):
        return None

    candidate: Dict[str, Any] = dict(raw)

    # Resolve name / title
    name = (
        raw.get("name")
        or raw.get("title")
        or raw.get("product_name")
        or raw.get("denominazione")
        or raw.get("n")
        or ""
    )
    candidate["name"] = str(name).strip()

    # Resolve brand
    brand = raw.get("brand") or raw.get("brands") or raw.get("marca") or raw.get("b") or ""
    candidate["brand"] = str(brand).strip()

    # Resolve barcode
    barcode = raw.get("barcode") or raw.get("code") or raw.get("ean") or raw.get("gtin") or ""
    candidate["barcode"] = str(barcode).strip()

    # Resolve nutrition table
    raw_table = raw.get("nutrition_table")
    if isinstance(raw_table, str):
        # Nested HTML table string
        raw_table = _extract_html_nutrition_table(raw_table)

    extracted_nutrients: Dict[str, Any] = {}
    if isinstance(raw_table, dict):
        for raw_k, raw_v in raw_table.items():
            clean_k = clean_label_key(raw_k)
            field = NUTRITION_FIELD_MAP.get(clean_k)

            if field == "calories_100g":
                parsed_e = parse_energy_value(raw_v)
                if parsed_e is not None:
                    extracted_nutrients["calories_100g"] = parsed_e
            elif field:
                parsed_num = extract_nutrient_from_string(raw_v)
                if parsed_num is not None:
                    extracted_nutrients[field] = parsed_num

    # Merge extracted nutrients into candidate (explicit nutrients override)
    for field_name, field_val in extracted_nutrients.items():
        candidate[field_name] = field_val

    # Also handle top-level energy if specified as string with kJ/kcal
    for kcal_key in ("energia", "energy", "valore energetico", "calories_100g", "calories"):
        if kcal_key in candidate and isinstance(candidate[kcal_key], str):
            e_parsed = parse_energy_value(candidate[kcal_key])
            if e_parsed is not None:
                candidate["calories_100g"] = e_parsed
                break

    return validate_and_normalize(candidate, source=source)


class SupermarketSpider:
    """
    Crawler for Italian supermarket catalogs with stealth headers
    and automatic HTML/JSON parsing into validated FoodRecords.
    """

    def __init__(
        self,
        base_url: str = "",
        parser_fn: Optional[Callable[[Any, str], Optional[FoodRecord]]] = None,
        session: Optional[Any] = None,
        timeout: int = 15,
        stealth: bool = True,
    ):
        self.base_url = base_url.rstrip("/")
        self.parser_fn = parser_fn or parse_supermarket_html_or_json
        self.timeout = timeout
        self.stealth = stealth
        self.session = session or self._create_stealth_session()

    def _create_stealth_session(self) -> requests.Session:
        """Create a requests session with realistic stealth browser headers."""
        s = requests.Session()
        s.headers.update({
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/128.0.0.0 Safari/537.36"
            ),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "Accept-Language": "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7",
            "Sec-Ch-Ua": '"Chromium";v="128", "Not;A=Brand";v="24", "Google Chrome";v="128"',
            "Sec-Ch-Ua-Mobile": "?0",
            "Sec-Ch-Ua-Platform": '"Windows"',
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "same-origin",
            "Sec-Fetch-User": "?1",
            "Upgrade-Insecure-Requests": "1",
        })
        return s

    def fetch(self, url: str) -> str:
        """Fetch content for a URL or file path, returning string body."""
        # 1. Local filesystem path check
        path = Path(url)
        if path.exists() and path.is_file():
            return path.read_text(encoding="utf-8", errors="ignore")

        # 2. Complete relative URL if base_url is set
        full_url = url
        if self.base_url and not (url.startswith("http://") or url.startswith("https://")):
            full_url = urllib.parse.urljoin(self.base_url + "/", url.lstrip("/"))

        # 3. Use Fetcher / StealthyFetcher if available and requested
        if HAS_SCRAPLING_FETCHERS and self.stealth and Fetcher is not None:
            try:
                res = Fetcher().get(full_url, timeout=self.timeout)
                return getattr(res, "text", str(res))
            except Exception as e:
                logger.debug("Scrapling Fetcher failed, falling back to requests: %s", e)

        # 4. Standard requests session with stealth headers
        resp = self.session.get(full_url, timeout=self.timeout)
        resp.raise_for_status()
        return resp.text

    def extract_links(self, html_content: str, current_url: str = "") -> List[str]:
        """Extract product or catalog links from an HTML document."""
        links: List[str] = []
        base = current_url or self.base_url

        if BeautifulSoup is not None:
            soup = BeautifulSoup(html_content, "html.parser")
            for a in soup.find_all("a", href=True):
                href = a["href"].strip()
                if not href or href.startswith(("#", "javascript:", "mailto:", "tel:")):
                    continue
                full_url = urllib.parse.urljoin(base + "/" if base and not base.endswith("/") else base, href)
                if full_url not in links:
                    links.append(full_url)
        else:
            raw_hrefs = re.findall(r'href=["\']([^"\']+)["\']', html_content, re.IGNORECASE)
            for href in raw_hrefs:
                href = href.strip()
                if not href or href.startswith(("#", "javascript:", "mailto:", "tel:")):
                    continue
                full_url = urllib.parse.urljoin(base + "/" if base and not base.endswith("/") else base, href)
                if full_url not in links:
                    links.append(full_url)

        return links

    def crawl(self, start_url: str, limit: int = 50) -> List[FoodRecord]:
        """Crawl from start_url extracting products up to limit."""
        records: List[FoodRecord] = []
        visited: Set[str] = set()
        queue: List[str] = [start_url]

        while queue and len(records) < limit:
            url = queue.pop(0)
            if url in visited:
                continue
            visited.add(url)

            try:
                content = self.fetch(url)
            except Exception as e:
                logger.debug("Failed fetching %s: %s", url, e)
                continue

            if not content:
                continue

            # Check if this page is a product
            rec = self.parser_fn(content, "SCRAPLING_GDO")
            if rec is not None:
                records.append(rec)
                if len(records) >= limit:
                    break

            # Extract additional links for the queue
            if len(records) < limit:
                new_links = self.extract_links(content, current_url=url)
                product_links: List[str] = []
                other_links: List[str] = []
                for link in new_links:
                    if link not in visited and link not in queue:
                        if any(p in link.lower() for p in ("/prodott", "/prodotto/", "/articolo/", "/item/", "/p/", "/spesa/")):
                            product_links.append(link)
                        else:
                            other_links.append(link)
                # Prioritize product links at the front of queue while preserving order
                queue = product_links + queue + other_links

        return records


def scrape_supermarket_catalog(target: str, limit: int = 50) -> List[FoodRecord]:
    """
    Extract validated FoodRecords from a local file, directory, or remote supermarket catalog.

    Supports offline deterministic testing and live scraping.
    """
    records: List[FoodRecord] = []
    path = Path(target)

    # 1. Local filesystem path handling
    if path.exists():
        if path.is_file():
            if path.suffix.lower() == ".json":
                try:
                    with open(path, "r", encoding="utf-8") as f:
                        data = json.load(f)
                    items = data if isinstance(data, list) else [data]
                    for item in items:
                        rec = parse_supermarket_html_or_json(item)
                        if rec is not None:
                            records.append(rec)
                            if len(records) >= limit:
                                break
                except Exception as e:
                    logger.warning("Error reading JSON file %s: %s", path, e)
            elif path.suffix.lower() in (".html", ".htm"):
                try:
                    content = path.read_text(encoding="utf-8", errors="ignore")
                    rec = parse_supermarket_html_or_json(content)
                    if rec is not None:
                        records.append(rec)
                    else:
                        spider = SupermarketSpider(base_url=str(path))
                        records = spider.crawl(str(path), limit=limit)
                except Exception as e:
                    logger.warning("Error reading HTML file %s: %s", path, e)

        elif path.is_dir():
            for file in sorted(path.rglob("*")):
                if len(records) >= limit:
                    break
                if not file.is_file():
                    continue

                if file.suffix.lower() == ".json":
                    try:
                        with open(file, "r", encoding="utf-8") as f:
                            data = json.load(f)
                        items = data if isinstance(data, list) else [data]
                        for it in items:
                            rec = parse_supermarket_html_or_json(it)
                            if rec is not None:
                                records.append(rec)
                                if len(records) >= limit:
                                    break
                    except Exception:
                        continue

                elif file.suffix.lower() in (".html", ".htm"):
                    try:
                        content = file.read_text(encoding="utf-8", errors="ignore")
                        rec = parse_supermarket_html_or_json(content)
                        if rec is not None:
                            records.append(rec)
                    except Exception:
                        continue

        return records

    # 2. Remote URL handling via SupermarketSpider
    spider = SupermarketSpider(base_url=target)
    return spider.crawl(start_url=target, limit=limit)
