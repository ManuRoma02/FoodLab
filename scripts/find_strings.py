import json

path = r'C:\Users\developer\.gemini\antigravity\brain\b68190cc-4231-4f36-9ad7-70107fe5cc36\.system_generated\logs\transcript_full.jsonl'

with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        if 'widget_mascot_status' in line:
            print(line[:400])
            break
