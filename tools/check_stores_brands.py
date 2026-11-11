import urllib.request
import json
import time

stores = ['esselunga', 'lidl', 'eurospin', 'conad', 'coop', 'md', 'aldi', 'carrefour', 'penny']
headers = {'User-Agent': 'FoodLab - Android - Version 1.0.46 (contact: info@foodlab.app)'}

print("=== CHECKING ITALIAN STORES IN OPEN FOOD FACTS ===")
for store in stores:
    url = f'https://it.openfoodfacts.org/store/{store}.json?page_size=1'
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode('utf-8'))
            count = data.get('count', 0)
            print(f'Store {store:12} -> {count:6} products')
    except Exception as e:
        print(f'Store {store:12} -> err: {e}')
    time.sleep(0.5)

brands = ['barilla', 'mulino-bianco', 'ferrero', 'kinder', 'parmalat', 'granarolo', 'milbona', 'tre-mulini', 'land', 'dolciando']
print("\n=== CHECKING POPULAR BRANDS IN OPEN FOOD FACTS ===")
for brand in brands:
    url = f'https://it.openfoodfacts.org/brand/{brand}.json?page_size=1'
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode('utf-8'))
            count = data.get('count', 0)
            print(f'Brand {brand:15} -> {count:6} products')
    except Exception as e:
        print(f'Brand {brand:15} -> err: {e}')
    time.sleep(0.5)
