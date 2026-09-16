import os, glob, json, re

trans_files = glob.glob(r'C:\Users\developer\.cursor\projects\c-Users-developer-Dropbox-FoodLab\agent-transcripts\**\*.jsonl', recursive=True)
trans_files.append(r'C:\Users\developer\.gemini\antigravity\brain\b68190cc-4231-4f36-9ad7-70107fe5cc36\.system_generated\logs\transcript_full.jsonl')

files_to_find = [
    'MascotComponents.kt',
    'DashboardComponents.kt',
    'DashboardScreen.kt',
    'Dialogs.kt',
    'AnalyticsScreen.kt',
    'CreateRecipeScreen.kt',
    'FoodSearchScreen.kt',
    'SettingsScreen.kt',
    'SearchOptimizer.kt',
    'AppDatabase.kt',
    'Daos.kt',
    'Repository.kt',
    'DashboardViewModel.kt',
    'PedometerViewModel.kt',
    'FoodSearchViewModel.kt',
    'FoodLabApplication.kt',
    'MascotComponentsTest.kt'
]

results = {}

for tf in trans_files:
    try:
        with open(tf, 'r', encoding='utf-8', errors='ignore') as f:
            for line in f:
                if not any(fn in line for fn in files_to_find):
                    continue
                try:
                    obj = json.loads(line)
                    # Look for tool results or read_file/view_file outputs
                    text = json.dumps(obj)
                    for fn in files_to_find:
                        # Find if there is file content
                        # Common pattern in cursor: {"type":"tool_result", ... "content":"..."} or {"tool_name":"read_file", ...}
                        if fn in text and ('package com.example' in text or 'import com.example' in text):
                            # extract content string
                            def extract_code(val):
                                if isinstance(val, str):
                                    if 'package com.example' in val and len(val) > 200:
                                        if fn not in results or len(val) > len(results[fn]):
                                            results[fn] = val
                                elif isinstance(val, dict):
                                    for v in val.values(): extract_code(v)
                                elif isinstance(val, list):
                                    for v in val: extract_code(v)

                            extract_code(obj)
                except:
                    pass
    except Exception as e:
        print(f"Error reading {tf}: {e}")

print(f"Recovered full code for {len(results)}/{len(files_to_find)} files:")
for k, v in results.items():
    print(f"  - {k} ({len(v)} chars, {len(v.splitlines())} lines)")
