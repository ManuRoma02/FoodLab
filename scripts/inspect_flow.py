import json

path = r'C:\Users\developer\.cursor\projects\c-Users-developer-Dropbox-FoodLab\agent-transcripts\36716cbc-03ca-44c2-b12e-3c4acf1f635b\36716cbc-03ca-44c2-b12e-3c4acf1f635b.jsonl'
found_assistant = False
with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        obj = json.loads(line)
        if found_assistant:
            print("Next message role:", obj.get('role'))
            print(json.dumps(obj, indent=2)[:600])
            break
        if 'CreaDatabaseImporter.kt' in line and obj.get('role') == 'assistant':
            found_assistant = True
