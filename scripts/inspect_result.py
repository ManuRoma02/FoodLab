import json

path = r'C:\Users\developer\.cursor\projects\c-Users-developer-Dropbox-FoodLab\agent-transcripts\36716cbc-03ca-44c2-b12e-3c4acf1f635b\36716cbc-03ca-44c2-b12e-3c4acf1f635b.jsonl'
with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        if 'CreaDatabaseImporter.kt' in line and 'tool_result' in line:
            obj = json.loads(line)
            print(json.dumps(obj, indent=2)[:500])
            break
