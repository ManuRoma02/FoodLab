import os, json

brain_dir = r'C:\Users\developer\.gemini\antigravity\brain'
for cid in os.listdir(brain_dir):
    p = os.path.join(brain_dir, cid, '.system_generated', 'logs', 'transcript_full.jsonl')
    if not os.path.exists(p): continue
    with open(p, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            if 'object MealTypes' in line or 'class MealTypes' in line or 'enum class MealType' in line or 'val MealTypes' in line:
                print("Found in", cid)
                print(line[:300])
                break
