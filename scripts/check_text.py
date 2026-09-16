import json

path = r'C:\Users\developer\.cursor\projects\c-Users-developer-Dropbox-FoodLab\agent-transcripts\36716cbc-03ca-44c2-b12e-3c4acf1f635b\36716cbc-03ca-44c2-b12e-3c4acf1f635b.jsonl'
with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        obj = json.loads(line)
        if obj.get('role') == 'user':
            text = obj['message']['content'][0].get('text', '')
            if 'package com.example' in text:
                print("Found file content in user message! Prefix:")
                print(text[:300])
                print("Total length:", len(text))
                break
