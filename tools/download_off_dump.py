"""Download Open Food Facts dataset dump with chunked stream and progress indicator."""

from __future__ import annotations

import os
from pathlib import Path
import time
import urllib.request
import sys

URL = "https://static.openfoodfacts.org/data/en.openfoodfacts.org.products.csv.gz"
DEST = Path("data_cache/en.openfoodfacts.org.products.csv.gz")

def download_dump():
    DEST.parent.mkdir(parents=True, exist_ok=True)
    
    # Check if already fully downloaded
    if DEST.exists() and DEST.stat().st_size > 1_400_000_000:
        print(f"File already downloaded: {DEST} ({DEST.stat().st_size / (1024*1024):.1f} MB)")
        return

    print(f"Starting download of Open Food Facts CSV dump from:\n{URL}\nDestination: {DEST}")
    t0 = time.time()
    
    req = urllib.request.Request(URL, headers={"User-Agent": "FoodLab-DatasetBuilder/1.0 (contact: info@foodlab.app)"})
    
    with urllib.request.urlopen(req, timeout=60) as resp:
        total_size = int(resp.headers.get("Content-Length", 0))
        downloaded = 0
        chunk_size = 512 * 1024  # 512 KB chunks
        
        with open(DEST, "wb") as out:
            last_print = time.time()
            while True:
                chunk = resp.read(chunk_size)
                if not chunk:
                    break
                out.write(chunk)
                downloaded += len(chunk)
                
                now = time.time()
                if now - last_print >= 2.0:
                    last_print = now
                    dt = now - t0
                    speed = (downloaded / (1024*1024)) / dt if dt > 0 else 0
                    pct = (downloaded / total_size * 100) if total_size > 0 else 0
                    print(f"[{pct:5.1f}%] Downloaded {downloaded / (1024*1024):6.1f} / {total_size / (1024*1024):6.1f} MB ({speed:4.1f} MB/s)")
    
    total_time = time.time() - t0
    print(f"\nDownload complete in {total_time:.1f}s ({DEST.stat().st_size / (1024*1024):.1f} MB)")

if __name__ == "__main__":
    download_dump()
