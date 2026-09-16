import json

path = r'C:\Users\developer\.cursor\projects\c-Users-developer-Dropbox-FoodLab\agent-transcripts\36716cbc-03ca-44c2-b12e-3c4acf1f635b\36716cbc-03ca-44c2-b12e-3c4acf1f635b.jsonl'
with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        obj = json.loads(line)
        if obj.get('role') == 'user':
            msg = obj.get('message', {})
            content = msg.get('content', [])
            if isinstance(content, list) and len(content) > 0:
                first = content[0]
                if isinstance(first, dict):
                    print("User content part type:", first.get('type'), list(first.keys()))
                    if first.get('type') != 'text':
                        print(json.dumps(first, indent=2)[:400])
                        break
