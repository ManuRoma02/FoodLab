import json

path = r'C:\Users\developer\.cursor\projects\c-Users-developer-Dropbox-FoodLab\agent-transcripts\36716cbc-03ca-44c2-b12e-3c4acf1f635b\36716cbc-03ca-44c2-b12e-3c4acf1f635b.jsonl'
roles = set()
with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        obj = json.loads(line)
        roles.add(obj.get('role', obj.get('type')))
print("Roles/types:", roles)
