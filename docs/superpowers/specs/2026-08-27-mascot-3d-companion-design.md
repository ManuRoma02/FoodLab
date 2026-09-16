# FoodLab 3D mascot companion

Date: 2026-08-27

## Goal

Upgrade the current monster + funny ghost duo into a **restyled, skinned 3D companion** on the **in-app home page**, and restyle the **home-screen widget** (new layout and look, same data). The mascot appears **only** on those two surfaces.

## Non-goals

- Live 3D inside the Android widget (widgets cannot run WebView/WebGL).
- Mascot on any screen other than in-app home.
- A second full skeleton for the ghost.
- New widget metrics or data fields.
- Runtime 3D generation (Tripo/Meshy run offline only).

## Decisions (locked)

| Topic | Choice |
| --- | --- |
| Surfaces | In-app home (live 3D) + home-screen widget (looping image) |
| Companion | UI-driven animation states on home only |
| Look | Same duo, restyled: more skeletal monster, cuter/funnier ghost |
| Rigs | Monster = full skeleton; ghost = simple floaty extra |
| Pipeline | Generated GLB + Three.js WebView (approach 1) |
| Widget | New layout/skin; same data; looping idle render, not live 3D |

## Architecture

Two surfaces, one character set.

**In-app home** loads a WebView that plays `duo.glb`: a skinned monster plus a simple ghost. Kotlin sends companion **state** names into the page. The page crossfades clips on the monster and applies a light matching motion on the ghost (bob, squash). Leaving home pauses or destroys the WebView.

**Home-screen widget** keeps today’s data and update triggers. It uses a new layout (spacing, color, type, rounded container) and `idle_loop.webp` — a looping render of the restyled duo. No GLB, no `setState`.

Ship the viewer with the existing HTML path (`scripts/write_mascot_html.py`, `scripts/build_inlined_3d_html.py`). Put runtime files next to that pipeline (for example `assets/mascot/` in the Android module that already hosts the home WebView). Follow the real source tree at implementation time; do not invent a new module.

```
Concept art (restyle)
        │
        ▼
Tripo: monster mesh + auto-rig; simple ghost mesh
        │
        ▼
Blender: cleanup, clips, glTF export, idle movie
        │
        ├──────────────► duo.glb  ──► Home WebView (Three.js)
        └──────────────► idle_loop.webp ──► Widget ImageView
```

## Components

### 1. Character assets

Offline artifacts only:

- `duo.glb` — skinned monster, simple ghost, named clips.
- `idle_loop.webp` — looping idle for widget and for 2D fallback on home.
- Current files stay as look reference until replaced: `mascot_duo_full_body.jpg`, `mascot_duo_rigging.png`, `mascot_waving.gif`, `mascot_waving.webp`.

The app never calls a 3D generation API.

### 2. Mascot viewer (HTML/JS)

Owns the Three.js scene: load GLB, lights, camera framed for the home card, `AnimationMixer` on the monster, a short bob/look on the ghost.

**JS API (only):** `setState(name: string)`

- Known names play the matching clip.
- Unknown names are ignored.
- Missing clip → `idle`.
- `idle` loops. `cook` loops until the host sends another state (normally `idle` when progress ends). Other clips play once, then return to `idle`, unless a newer `setState` arrived.
- Rapid calls: **latest state wins**; no queue.

### 3. Home mascot host (Android)

WebView **only** on the in-app home page. Maps real home-page events to state names. If the WebView is destroyed, the next home visit reloads and starts at `idle`.

### 4. Home widget

Same fields and update triggers as today. New hierarchy, padding, type, and container. Mascot image on one side, data on the other. A widget update does not change animation state; it only refreshes layout + the bundled loop (or a newly shipped `idle_loop.webp`).

## Look

Stylized cartoony 3D, not horror and not realistic anatomy.

- **Monster:** funny skeleton; readable bones; big head; silhouette that holds up at widget size.
- **Ghost:** round, simple, cute, slightly translucent; few parts; sidekick, not a second hero.
- **Home camera:** three-quarter view, framed for the home card. Soft light plus one accent from the existing FoodLab home palette.
- **States** change pose and motion, not costume.

## Animation contract

Clip names in `duo.glb` **are** these strings (no aliases):

| State | Clip | Home trigger (map existing events; unlisted events stay idle) |
| --- | --- | --- |
| `idle` | loop | Default; after one-shot clips |
| `think` | one-shot | Loading / waiting on home |
| `cook` | loop while progress is active; `setState("idle")` when progress ends | Cooking / in-progress work on home |
| `success` | one-shot | Successful home action |
| `error` | one-shot | Failed home action |
| `celebrate` | one-shot | Notable win (e.g. goal / streak), not every small success |
| `wave` | one-shot, rare | First home open of a session, or explicit welcome |

Ghost motion is procedural, not a second Mixamo rig: faster bob on `celebrate`, pull-back on `error`, otherwise a slow float.

## Data flow

1. Home opens → WebView loads → GLB loads → `idle`.
2. Home event → Kotlin `setState(name)` over the JS bridge.
3. Viewer plays monster clip + ghost accent → returns to `idle` unless superseded.
4. User leaves home → renderer pauses; no background GPU.
5. Widget updates independently with the same data as today and always shows `idle_loop.webp`.

## Error handling

| Failure | Behavior |
| --- | --- |
| GLB missing or corrupt | Home shows `idle_loop.webp` (same as widget), not a blank WebView. Log once. No tight retry loop. |
| WebGL unavailable or page crash | 2D fallback for that session. |
| Unknown `setState` | Ignore. |
| Missing clip | `idle`. |
| Clip name mismatch in GLB | Boot in `idle`; play only clips that exist. Kotlin does not crash. |
| Widget image missing | Keep last image or bundled placeholder. Do not crash. Do not drop existing widget fields. Empty values use today’s empty treatment. |

## Testing

- **Assets:** `duo.glb` in a glTF viewer — monster skins cleanly, ghost is a separate simple object, all seven clips exist. `idle_loop.webp` readable at widget size.
- **Home:** idle on load; each `setState` from a debug hook or real events; crossfade; return to idle; latest-state-wins.
- **Fallback:** renamed GLB or broken WebGL → 2D loop. Leave home → renderer paused.
- **Widget:** new layout, same data, looping image; empty field matches today; missing image does not crash.
- **Regression:** no mascot/WebView on non-home screens; widget still updates on the same triggers.

## Skill stack (implementation)

Use these skills in this order; do not install low-install Blender/mascot skills.

1. `gamedev-skills/awesome-gamedev-agent-skills@create-game-assets` — restyle concept from current duo refs.
2. `majidmanzarpour/threejs-game-skills@threejs-3d-generator` — image-to-3D, auto-rig monster, simple ghost, GLB (Tripo; needs `TRIPO_API_KEY`). Backup: `playableintelligence/game-creator@meshyai`.
3. `freshtechbro/claudedesignskills@blender-web-pipeline` — cleanup, clip names, web-optimized glTF, idle movie → webp.
4. `gamedev-skills/awesome-gamedev-agent-skills@threejs-gltf-loading` and `@threejs-scene-setup` — WebView playback.

Skip: `mascot-logo-reconstruction` (low stars), `mascot-puppet-horror` (wrong tone), 2D “3D-looking image” skills.

## Implementation order

1. Restyle concept stills (monster + ghost) from current references.
2. Generate and rig `duo.glb`; export `idle_loop.webp`.
3. Upgrade mascot HTML viewer: load GLB, `setState`, fallback image.
4. Wire home-page events to `setState`; pause off-screen.
5. Restyle widget layout; swap in `idle_loop.webp`; keep data bindings.

## Success

Home shows a restyled, skinned monster and a cute floaty ghost that reacts to home UI states. The widget looks new, shows the same data as today, and displays a looping still of that duo. Nothing else in the app gains a mascot.
