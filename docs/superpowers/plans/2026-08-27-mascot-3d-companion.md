# 3D Mascot Companion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a restyled skinned monster + cute floaty ghost on the in-app home WebView, driven by UI states, and restyle the home-screen widget (same data, looping idle image).

**Architecture:** Offline pipeline produces `duo.glb` and `idle_loop.webp`. Home loads a Three.js WebView that plays named clips via `setState(name)`. The widget never runs 3D; it uses a new layout plus the looping webp. Pure state-machine logic lives in testable Python/Kotlin units so the viewer and Android host stay thin.

**Tech Stack:** Android (existing FoodLab module), WebView + Three.js r184, glTF 2.0, Python 3 for HTML generation and unit tests, Tripo (or Meshy backup) + Blender for assets.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-27-mascot-3d-companion-design.md` (verbatim).
- Surfaces: in-app home (live 3D) + home-screen widget (looping image) only.
- No live 3D in the widget. No mascot on any other screen.
- Monster = full skeleton. Ghost = simple floaty extra (procedural bob, not a second Mixamo rig).
- Clip names (no aliases): `idle`, `think`, `cook`, `success`, `error`, `celebrate`, `wave`.
- `idle` loops. `cook` loops until host sends another state. Other clips are one-shot then `idle` unless superseded. Latest `setState` wins; no queue.
- Unknown `setState` ignored. Missing clip → `idle`. GLB/WebGL failure → show `idle_loop.webp`, log once, no tight retry.
- App never calls Tripo/Meshy at runtime. `TRIPO_API_KEY` is offline-only.
- JS API is only `setState(name: string)`.
- Widget keeps today’s fields and update triggers; restyle layout/skin only.
- Look: cartoony, not horror. Funny skeletal monster, cute round translucent ghost.
- This workspace often has **no `.git`** (only `.git_backup`). If `git status` fails, skip every Commit step and continue.
- Dropbox may leave sources online-only. Task 1 must hydrate and map files before modifying Android UI.

---

## File structure

| Path | Responsibility |
| --- | --- |
| `docs/superpowers/mascot-source-map.md` | Exact Android paths discovered in Task 1 |
| `scripts/mascot_state.py` | Pure clip/state reducer used by tests and documented for the JS port |
| `scripts/test_mascot_state.py` | Unit tests for the reducer |
| `scripts/write_mascot_html.py` | Generates the WebView HTML (keep existing name; replace body with GLB viewer) |
| `scripts/build_inlined_3d_html.py` | Existing inliner; keep calling it after HTML regen |
| `src/main/assets/mascot/duo.glb` | Skinned monster + simple ghost + seven clips (final assets dir may change per source map `ASSETS_DIR`) |
| `src/main/assets/mascot/idle_loop.webp` | Widget + home 2D fallback |
| `src/main/assets/mascot/index.html` | Generated viewer (or whatever path `write_mascot_html.py` already writes — follow source map `VIEWER_HTML`) |
| Kotlin `MascotState.kt` + `MascotStateMapper.kt` next to home code | Typed states + event mapping |
| Kotlin `MascotWebViewHost` in home screen file | WebView, JS bridge, pause, fallback ImageView |
| Widget layout + provider (source map) | New chrome, same data, `idle_loop.webp` |

Until Task 1 finishes, do not invent a new Gradle module. Put Kotlin files in the same package as the existing home screen.

---

### Task 1: Hydrate Dropbox and write the source map

**Files:**
- Create: `docs/superpowers/mascot-source-map.md`
- Read: `settings.gradle.kts`, `build.gradle.kts`, `scripts/write_mascot_html.py`, `scripts/build_inlined_3d_html.py`

**Interfaces:**
- Consumes: nothing
- Produces: source map keys `HOME_FILE`, `WIDGET_LAYOUT`, `WIDGET_PROVIDER`, `ASSETS_DIR`, `VIEWER_HTML`, `APPLICATION_ID`, `PACKAGE`

- [ ] **Step 1: Confirm Dropbox files are local**

Run (PowerShell, repo root):

```powershell
Get-ChildItem -Name
Test-Path src
Test-Path app
Get-Content settings.gradle.kts -TotalCount 40
```

Expected: listing includes `build.gradle.kts`, `scripts`, mascot images. If `Get-Content` throws “cloud provider is not running”, make Dropbox files available offline for `FoodLab`, then re-run until `settings.gradle.kts` prints.

- [ ] **Step 2: Find home, widget, and current mascot HTML**

Run:

```powershell
Get-ChildItem -Recurse -File -Include *.kt,*.xml,*.html -ErrorAction SilentlyContinue |
  Where-Object { $_.FullName -notmatch '\\build\\|\\.gradle\\' } |
  Select-Object -ExpandProperty FullName
```

Then search:

```powershell
Select-String -Path (Get-ChildItem -Recurse -Include *.kt,*.xml -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notmatch '\\build\\' }) -Pattern 'WebView|AppWidget|GlanceAppWidget|mascot|waving' |
  Select-Object -First 80 Path, LineNumber, Line
```

Expected: at least one home UI file, one widget layout or Glance file, and the HTML output path used by `write_mascot_html.py`.

- [ ] **Step 3: Write the source map**

Create `docs/superpowers/mascot-source-map.md` filled with real paths (example shape — replace with discovered values):

```markdown
# Mascot source map

- HOME_FILE: src/main/java/<pkg>/HomeScreen.kt
- WIDGET_LAYOUT: src/main/res/layout/foodlab_widget.xml
- WIDGET_PROVIDER: src/main/java/<pkg>/FoodLabWidget.kt
- ASSETS_DIR: src/main/assets/mascot
- VIEWER_HTML: src/main/assets/mascot/index.html
- APPLICATION_ID: <from build.gradle.kts>
- PACKAGE: <home file package>
- WRITE_MASCOT_HTML: scripts/write_mascot_html.py
- BUILD_INLINED_HTML: scripts/build_inlined_3d_html.py
```

If there is no `src/` and sources live under another module directory, use that directory everywhere later tasks say `src/main/...`.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/mascot-source-map.md
git commit -m "docs: map home and widget files for 3D mascot"
```

Skip if not a git repository.

---

### Task 2: Pure mascot state reducer (TDD)

**Files:**
- Create: `scripts/mascot_state.py`
- Test: `scripts/test_mascot_state.py`

**Interfaces:**
- Consumes: clip-name contract from the spec
- Produces: `KNOWN_STATES`, `LOOPING_STATES`, `resolve_set_state(current: str, incoming: str, available_clips: set[str]) -> str`, `after_clip_end(playing: str) -> str`

- [ ] **Step 1: Write the failing tests**

Create `scripts/test_mascot_state.py`:

```python
import unittest

from mascot_state import after_clip_end, resolve_set_state


class ResolveSetStateTest(unittest.TestCase):
    clips = {
        "idle",
        "think",
        "cook",
        "success",
        "error",
        "celebrate",
        "wave",
    }

    def test_known_state_becomes_current(self):
        self.assertEqual(
            resolve_set_state("idle", "think", self.clips),
            "think",
        )

    def test_unknown_name_keeps_current(self):
        self.assertEqual(
            resolve_set_state("idle", "dance", self.clips),
            "idle",
        )

    def test_missing_clip_falls_back_to_idle(self):
        self.assertEqual(
            resolve_set_state("idle", "think", {"idle"}),
            "idle",
        )

    def test_latest_wins_not_queue(self):
        mid = resolve_set_state("idle", "think", self.clips)
        self.assertEqual(resolve_set_state(mid, "error", self.clips), "error")

    def test_cook_stays_cook(self):
        self.assertEqual(
            resolve_set_state("idle", "cook", self.clips),
            "cook",
        )


class AfterClipEndTest(unittest.TestCase):
    def test_idle_stays_idle(self):
        self.assertEqual(after_clip_end("idle"), "idle")

    def test_cook_stays_cook(self):
        self.assertEqual(after_clip_end("cook"), "cook")

    def test_oneshot_returns_idle(self):
        for name in ("think", "success", "error", "celebrate", "wave"):
            self.assertEqual(after_clip_end(name), "idle")


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `python scripts/test_mascot_state.py -v`

Expected: FAIL with `ModuleNotFoundError: No module named 'mascot_state'` or import error.

- [ ] **Step 3: Write minimal implementation**

Create `scripts/mascot_state.py`:

```python
KNOWN_STATES = frozenset(
    {"idle", "think", "cook", "success", "error", "celebrate", "wave"}
)
LOOPING_STATES = frozenset({"idle", "cook"})


def resolve_set_state(current: str, incoming: str, available_clips: set[str]) -> str:
    if incoming not in KNOWN_STATES:
        return current
    if incoming not in available_clips:
        return "idle" if "idle" in available_clips else current
    return incoming


def after_clip_end(playing: str) -> str:
    if playing in LOOPING_STATES:
        return playing
    return "idle"
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `python scripts/test_mascot_state.py -v`

Expected: `OK` and all tests `ok`.

- [ ] **Step 5: Commit**

```bash
git add scripts/mascot_state.py scripts/test_mascot_state.py
git commit -m "test: add mascot clip state reducer"
```

---

### Task 3: Restyle concept stills

**Files:**
- Create: `docs/mascot/concept_monster.png`, `docs/mascot/concept_ghost.png`, `docs/mascot/concept_duo_three_quarter.png`
- Read: `mascot_duo_full_body.jpg`, `mascot_duo_rigging.png`

**Interfaces:**
- Consumes: current duo look
- Produces: three stills that Tripo/Meshy will take as image-to-3D input

- [ ] **Step 1: Install look skill (if missing)**

```bash
npx skills add gamedev-skills/awesome-gamedev-agent-skills@create-game-assets -g -y
```

Follow that skill: treat these images as drafts, same characters, restyled.

- [ ] **Step 2: Generate three stills**

Use Cursor `GenerateImage` (or the create-game-assets pipeline) with `mascot_duo_full_body.jpg` as reference.

Prompt for `concept_duo_three_quarter.png` (1:1):

```
Stylized cartoony 3D character duo, three-quarter view, full body, plain studio background.
Primary: funny skeleton monster, big head, readable bones, friendly not scary, clear silhouette.
Sidekick: small round cute ghost, slightly translucent, few parts, floating beside the monster's shoulder.
Soft light, one warm kitchen accent color. Not horror, not realistic anatomy, not extra characters.
```

Prompt for `concept_monster.png`: same monster, T-pose / A-pose, full body, no ghost.

Prompt for `concept_ghost.png`: ghost alone, simple blob + face, no skeleton.

Save under `docs/mascot/`.

- [ ] **Step 3: Gate**

Reject and regenerate if: horror, extra limbs, ghost as large as the monster, unreadable at ~128px, or a new species that is not the current duo.

- [ ] **Step 4: Commit**

```bash
git add docs/mascot/*.png
git commit -m "feat: restyle mascot concept stills for 3D pipeline"
```

---

### Task 4: Generate rigged `duo.glb`

**Files:**
- Create: `docs/mascot/duo_raw.glb` (Tripo/Meshy output before Blender cleanup)
- Test: visual check in a glTF viewer (no code test)

**Interfaces:**
- Consumes: `docs/mascot/concept_*.png`
- Produces: textured meshes; monster auto-rigged; ghost a separate simple object

- [ ] **Step 1: Install generator skill**

```bash
npx skills add majidmanzarpour/threejs-game-skills@threejs-3d-generator -g -y
```

- [ ] **Step 2: Probe Tripo key**

From the installed skill directory, run that skill’s probe (path as installed):

```bash
python <threejs-3d-generator>/scripts/threejs_3d_asset.py probe
```

Expected: `TRIPO_API_KEY=SET` or `MISSING`.

If `MISSING`, install backup and use Meshy instead:

```bash
npx skills add playableintelligence/game-creator@meshyai -g -y
```

Do not paste key values into files or chat.

- [ ] **Step 3: Image-to-3D monster**

Follow the installed skill’s image-to-3D + auto-rig commands using `docs/mascot/concept_monster.png`. Request humanoid auto-rig. Export GLB/FBX to `docs/mascot/monster_raw.glb`.

- [ ] **Step 4: Image-to-3D ghost (no full rig)**

Use `docs/mascot/concept_ghost.png`. Do **not** auto-rig as a second humanoid. Low poly, one mesh. Save `docs/mascot/ghost_raw.glb`.

- [ ] **Step 5: Gate**

Open both in a glTF viewer. Monster must have a skeleton and a skinned mesh. Ghost must be a simple mesh. If the ghost comes back with a full Mixamo rig, discard the rig and keep the mesh only in Task 5.

- [ ] **Step 6: Commit**

```bash
git add docs/mascot/monster_raw.glb docs/mascot/ghost_raw.glb
git commit -m "feat: add raw mascot meshes from image-to-3D"
```

---

### Task 5: Blender cleanup, clips, web export

**Files:**
- Create: `docs/mascot/duo.blend`, `ASSETS_DIR/duo.glb`, `ASSETS_DIR/idle_loop.webp` (`ASSETS_DIR` from source map; default `src/main/assets/mascot`)
- Read: blender-web-pipeline skill after install

**Interfaces:**
- Consumes: `monster_raw.glb`, `ghost_raw.glb`
- Produces: `duo.glb` with clip names exactly `idle|think|cook|success|error|celebrate|wave`; `idle_loop.webp` looping ~2–3s

- [ ] **Step 1: Install export skill**

```bash
npx skills add freshtechbro/claudedesignskills@blender-web-pipeline -g -y
```

- [ ] **Step 2: Assemble in Blender**

In Blender (follow blender-web-pipeline export settings: glTF 2.0, deformation bones only, animations on):

1. Import monster; keep armature + skinned mesh.
2. Import ghost mesh; parent to an empty at the monster’s shoulder; no second humanoid armature.
3. Name actions exactly: `idle`, `think`, `cook`, `success`, `error`, `celebrate`, `wave`.
   - `idle`: gentle breathing / weight shift, loop.
   - `think`: head tilt / hand to chin, one-shot.
   - `cook`: stirring / busy arms, loop.
   - `success`: small cheer, one-shot.
   - `error`: flinch, one-shot.
   - `celebrate`: bigger cheer, one-shot.
   - `wave`: wave, one-shot.
4. Ghost: no clips required. Motion is procedural in JS.
5. Export `duo.glb` to `ASSETS_DIR`.
6. Render a 2–3s idle movie, convert to looping `idle_loop.webp` in `ASSETS_DIR` (also copy to `docs/mascot/idle_loop.webp`).

If Mixamo is used for the monster, merge clips in NLA as: import first FBX with mesh, import later FBX animations, push-down actions, rename to the seven names, export glTF.

- [ ] **Step 3: Verify clips**

Open `duo.glb` in https://gltf.report/ or Blender. Confirm seven animation names. Confirm ghost is a separate node. Confirm monster skinning does not explode.

- [ ] **Step 4: Commit**

```bash
git add src/main/assets/mascot/duo.glb src/main/assets/mascot/idle_loop.webp docs/mascot/duo.blend docs/mascot/idle_loop.webp
git commit -m "feat: export skinned mascot glTF and widget idle loop"
```

Use `ASSETS_DIR` from the source map if it is not `src/main/assets/mascot`.

---

### Task 6: WebView viewer with `setState`

**Files:**
- Modify: `scripts/write_mascot_html.py` (replace generated page)
- Create: `ASSETS_DIR/index.html` (generated output; path = `VIEWER_HTML`)
- Read: `scripts/build_inlined_3d_html.py`
- Skills: `threejs-gltf-loading`, `threejs-scene-setup`

**Interfaces:**
- Consumes: `resolve_set_state` / `after_clip_end` rules; `duo.glb`; `idle_loop.webp`
- Produces: page that defines `window.setState = function(name) { ... }`, `window.__mascotReady`, `window.__mascotFallback(reason)`

- [ ] **Step 1: Install playback skills**

```bash
npx skills add gamedev-skills/awesome-gamedev-agent-skills@threejs-gltf-loading -g -y
npx skills add gamedev-skills/awesome-gamedev-agent-skills@threejs-scene-setup -g -y
```

- [ ] **Step 2: Rewrite `scripts/write_mascot_html.py` to emit this viewer**

The script must write `VIEWER_HTML`. Keep any existing CLI the repo already uses; only replace the HTML/JS body. Generated JS must implement the same rules as `scripts/mascot_state.py`.

Minimal page body (inline this in the Python writer; Three.js r184 + GLTFLoader from the same addons build the project already uses, or CDN `https://unpkg.com/three@0.184.0/` if the old page used a CDN):

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <style>
    html, body, #app { margin: 0; width: 100%; height: 100%; background: transparent; }
    #fallback {
      display: none; width: 100%; height: 100%; object-fit: contain;
    }
    canvas { display: block; width: 100%; height: 100%; }
  </style>
</head>
<body>
  <div id="app">
    <img id="fallback" alt="" src="idle_loop.webp" />
  </div>
  <script type="importmap">
  {
    "imports": {
      "three": "https://unpkg.com/three@0.184.0/build/three.module.js",
      "three/addons/": "https://unpkg.com/three@0.184.0/examples/jsm/"
    }
  }
  </script>
  <script type="module">
    import * as THREE from "three";
    import { GLTFLoader } from "three/addons/loaders/GLTFLoader.js";

    const KNOWN = new Set(["idle","think","cook","success","error","celebrate","wave"]);
    const LOOPING = new Set(["idle","cook"]);
    const GHOST_NAME = /ghost/i;

    const fallback = document.getElementById("fallback");
    let renderer, scene, camera, mixer, clock, monsterRoot, ghost, raf;
    let available = new Set();
    let actions = {};
    let current = "idle";
    let generation = 0;
    let paused = false;

    function showFallback(reason) {
      if (!window.__mascotDidLogFallback) {
        console.error("mascot fallback", reason);
        window.__mascotDidLogFallback = true;
      }
      if (renderer) renderer.setAnimationLoop(null);
      const canvas = document.querySelector("canvas");
      if (canvas) canvas.style.display = "none";
      fallback.style.display = "block";
      window.__mascotFallback = reason;
    }

    function ghostAmp(state) {
      if (state === "celebrate") return 1.6;
      if (state === "error") return 0.35;
      if (state === "cook") return 1.2;
      return 1.0;
    }

    function playState(name) {
      if (!KNOWN.has(name)) return;
      if (!available.has(name)) {
        name = available.has("idle") ? "idle" : current;
      }
      const nextGen = ++generation;
      const prev = actions[current];
      const next = actions[name];
      current = name;
      if (prev && prev !== next) prev.fadeOut(0.2);
      if (next) {
        next.reset();
        next.setLoop(LOOPING.has(name) ? THREE.LoopRepeat : THREE.LoopOnce, Infinity);
        next.clampWhenFinished = !LOOPING.has(name);
        next.fadeIn(0.2);
        next.play();
      }
      if (!LOOPING.has(name) && mixer) {
        const onFinished = (e) => {
          if (nextGen !== generation) return;
          if (e.action !== next) return;
          mixer.removeEventListener("finished", onFinished);
          playState("idle");
        };
        mixer.addEventListener("finished", onFinished);
      }
    }

    window.setState = function (name) {
      if (typeof name !== "string") return;
      playState(name);
    };
    window.setMascotPaused = function (value) {
      paused = !!value;
    };

    try {
      scene = new THREE.Scene();
      camera = new THREE.PerspectiveCamera(35, 1, 0.1, 100);
      camera.position.set(1.6, 1.2, 2.4);
      camera.lookAt(0, 0.9, 0);
      renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
      document.getElementById("app").appendChild(renderer.domElement);
      scene.add(new THREE.HemisphereLight(0xfff2e0, 0x334, 1.1));
      const key = new THREE.DirectionalLight(0xffd9a0, 1.2);
      key.position.set(2, 4, 3);
      scene.add(key);
      clock = new THREE.Clock();
      const loader = new GLTFLoader();
      loader.load(
        "duo.glb",
        (gltf) => {
          scene.add(gltf.scene);
          mixer = new THREE.AnimationMixer(gltf.scene);
          for (const clip of gltf.animations) {
            available.add(clip.name);
            actions[clip.name] = mixer.clipAction(clip);
          }
          gltf.scene.traverse((obj) => {
            if (GHOST_NAME.test(obj.name) && !ghost) ghost = obj;
          });
          playState("idle");
          window.__mascotReady = true;
          renderer.setAnimationLoop(() => {
            if (paused) return;
            const dt = clock.getDelta();
            if (mixer) mixer.update(dt);
            if (ghost) {
              const t = clock.elapsedTime;
              const a = ghostAmp(current);
              ghost.position.y = 0.15 * a * Math.sin(t * 2.2);
              if (current === "error") ghost.scale.setScalar(0.92);
              else ghost.scale.setScalar(1);
            }
            const w = renderer.domElement.clientWidth || 1;
            const h = renderer.domElement.clientHeight || 1;
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
            renderer.setSize(w, h, false);
            renderer.render(scene, camera);
          });
        },
        undefined,
        (err) => showFallback(String(err))
      );
    } catch (err) {
      showFallback(String(err));
    }

    window.addEventListener("error", () => showFallback("page-error"));
  </script>
</body>
</html>
```

If the existing writer already inlines Three.js, keep that mechanism and still emit `setState` / fallback / `setMascotPaused`.

- [ ] **Step 3: Generate and inline**

Run the same command the repo already uses, typically:

```powershell
python scripts/write_mascot_html.py
python scripts/build_inlined_3d_html.py
```

Expected: `VIEWER_HTML` exists and contains `setState` and `duo.glb`.

- [ ] **Step 4: Smoke-test in a browser**

Open `VIEWER_HTML` next to `duo.glb` and `idle_loop.webp` (Python http server from `ASSETS_DIR`):

```powershell
python -m http.server 8765 --directory <ASSETS_DIR>
```

Open `http://127.0.0.1:8765/`. Expected: idle plays. In DevTools: `setState('wave')` plays wave then idle; `setState('nope')` stays; `setState('cook')` loops until `setState('idle')`. Rename `duo.glb` temporarily and reload: fallback image, one console error.

- [ ] **Step 5: Commit**

```bash
git add scripts/write_mascot_html.py scripts/build_inlined_3d_html.py src/main/assets/mascot
git commit -m "feat: play skinned mascot GLB in home WebView page"
```

---

### Task 7: Kotlin mapper + home WebView host

**Files:**
- Create: `<PACKAGE>/mascot/MascotState.kt`, `<PACKAGE>/mascot/MascotStateMapper.kt` (`PACKAGE` from source map)
- Test: `<same module>/test/.../MascotStateMapperTest.kt` (existing test source set)
- Modify: `HOME_FILE` from source map

**Interfaces:**
- Consumes: home UI events already in `HOME_FILE`
- Produces: `enum class MascotState { Idle, Think, Cook, Success, Error, Celebrate, Wave }` with `fun clipName(): String`; `object MascotStateMapper` with `fun from(event: MascotHomeEvent): MascotState`; host methods `setState`, pause, fallback

- [ ] **Step 1: Write failing mapper tests**

Create `MascotHomeEvent` and tests in the module’s existing unit-test directory (if none, `src/test/java/<PACKAGE>/mascot/MascotStateMapperTest.kt`):

```kotlin
package <PACKAGE>.mascot

import org.junit.Assert.assertEquals
import org.junit.Test

class MascotStateMapperTest {
    @Test
    fun loadingMapsToThink() {
        assertEquals(MascotState.Think, MascotStateMapper.from(MascotHomeEvent.Loading))
    }

    @Test
    fun progressMapsToCook() {
        assertEquals(MascotState.Cook, MascotStateMapper.from(MascotHomeEvent.Progress))
    }

    @Test
    fun progressEndedMapsToIdle() {
        assertEquals(MascotState.Idle, MascotStateMapper.from(MascotHomeEvent.ProgressEnded))
    }

    @Test
    fun successMapsToSuccess() {
        assertEquals(MascotState.Success, MascotStateMapper.from(MascotHomeEvent.Success))
    }

    @Test
    fun errorMapsToError() {
        assertEquals(MascotState.Error, MascotStateMapper.from(MascotHomeEvent.Failure))
    }

    @Test
    fun streakMapsToCelebrateNotSuccess() {
        assertEquals(MascotState.Celebrate, MascotStateMapper.from(MascotHomeEvent.StreakOrGoal))
    }

    @Test
    fun firstOpenMapsToWave() {
        assertEquals(MascotState.Wave, MascotStateMapper.from(MascotHomeEvent.SessionWelcome))
    }

    @Test
    fun clipNamesMatchContract() {
        assertEquals("idle", MascotState.Idle.clipName())
        assertEquals("think", MascotState.Think.clipName())
        assertEquals("cook", MascotState.Cook.clipName())
        assertEquals("success", MascotState.Success.clipName())
        assertEquals("error", MascotState.Error.clipName())
        assertEquals("celebrate", MascotState.Celebrate.clipName())
        assertEquals("wave", MascotState.Wave.clipName())
    }
}
```

Replace `<PACKAGE>` with the source-map package.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test --tests <PACKAGE>.mascot.MascotStateMapperTest
```

On Windows: `gradlew.bat test --tests <PACKAGE>.mascot.MascotStateMapperTest`

Expected: FAIL (class not found).

- [ ] **Step 3: Implement mapper**

`MascotState.kt`:

```kotlin
package <PACKAGE>.mascot

enum class MascotState {
    Idle, Think, Cook, Success, Error, Celebrate, Wave;

    fun clipName(): String = when (this) {
        Idle -> "idle"
        Think -> "think"
        Cook -> "cook"
        Success -> "success"
        Error -> "error"
        Celebrate -> "celebrate"
        Wave -> "wave"
    }
}

enum class MascotHomeEvent {
    Loading,
    Progress,
    ProgressEnded,
    Success,
    Failure,
    StreakOrGoal,
    SessionWelcome,
}
```

`MascotStateMapper.kt`:

```kotlin
package <PACKAGE>.mascot

object MascotStateMapper {
    fun from(event: MascotHomeEvent): MascotState = when (event) {
        MascotHomeEvent.Loading -> MascotState.Think
        MascotHomeEvent.Progress -> MascotState.Cook
        MascotHomeEvent.ProgressEnded -> MascotState.Idle
        MascotHomeEvent.Success -> MascotState.Success
        MascotHomeEvent.Failure -> MascotState.Error
        MascotHomeEvent.StreakOrGoal -> MascotState.Celebrate
        MascotHomeEvent.SessionWelcome -> MascotState.Wave
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Same `./gradlew test --tests ...` command.

Expected: PASS.

- [ ] **Step 5: Wire home only**

In `HOME_FILE`:

1. Keep the mascot WebView on this screen only. Do not add it to other destinations.
2. Load `file:///android_asset/<mascot relative path>/index.html` (or the existing asset URL scheme the app already uses).
3. `webView.settings.javaScriptEnabled = true`
4. After `onPageFinished`, if this is the first home open of the process, evaluate `setState('wave')`; otherwise `setState('idle')`.
5. Map existing home loading/progress/success/failure/streak callbacks to:

```kotlin
webView.evaluateJavascript(
    "setState('${MascotStateMapper.from(event).clipName()}')",
    null
)
```

6. Add an `ImageView` (or Compose `Image`) behind/instead of the WebView, src = `idle_loop.webp` or a drawable copy, `visibility = GONE` by default.
7. `webChromeClient` / `onReceivedError` / JS `window.__mascotFallback`: show the ImageView, hide WebView. Log once with tag `Mascot`.
8. Do not retry GLB load in a loop.

If home is Compose `AndroidView`, wrap the same WebView logic there.

- [ ] **Step 6: Manual check**

Run the app, open home: idle or wave. Trigger a real load: think. Trigger failure: error then idle. Navigate to any other tab: no WebView/mascot.

- [ ] **Step 7: Commit**

```bash
git add src
git commit -m "feat: drive home mascot WebView from UI events"
```

---

### Task 8: Pause renderer off home

**Files:**
- Modify: `HOME_FILE` (lifecycle)

**Interfaces:**
- Consumes: `window.setMascotPaused(boolean)` from Task 6
- Produces: no GPU loop when home is not visible

- [ ] **Step 1: Pause and resume**

When the home fragment/composable leaves the composition or `onPause`:

```kotlin
webView.evaluateJavascript("setMascotPaused(true)", null)
webView.onPause()
```

When returning:

```kotlin
webView.onResume()
webView.evaluateJavascript("setMascotPaused(false)", null)
```

On destroy: `webView.destroy()` after `onPause`. Next visit reloads at idle.

- [ ] **Step 2: Verify**

Open home, then switch app or leave home. Logcat: no continuous `Mascot` / WebView GPU chatter. Return: idle resumes.

- [ ] **Step 3: Commit**

```bash
git add src
git commit -m "fix: pause mascot WebView when home is not visible"
```

---

### Task 9: Restyle widget (same data)

**Files:**
- Modify: `WIDGET_LAYOUT` and `WIDGET_PROVIDER` from source map
- Add: widget mascot image = `idle_loop.webp` (RemoteViews needs a drawable: copy or convert to `res/drawable-nodpi/mascot_idle_loop.webp`)

**Interfaces:**
- Consumes: existing widget data model (do not add fields)
- Produces: new spacing, type, rounded container; mascot on one side; data on the other

- [ ] **Step 1: Snapshot current widget fields**

List every `RemoteViews.setTextViewText` / Glance text currently set. That list is the data contract. Do not add or remove keys.

- [ ] **Step 2: New layout**

XML pattern (adapt IDs to the existing ones):

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp"
    android:gravity="center_vertical"
    android:background="@drawable/widget_mascot_bg"
    android:orientation="horizontal">

    <ImageView
        android:id="@+id/widget_mascot"
        android:layout_width="72dp"
        android:layout_height="72dp"
        android:layout_marginEnd="12dp"
        android:scaleType="fitCenter"
        android:src="@drawable/mascot_idle_loop"
        android:contentDescription="@null" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">
        <!-- Re-host existing text views with the SAME ids so the provider does not change bindings -->
    </LinearLayout>
</LinearLayout>
```

`widget_mascot_bg`: rounded rectangle, color from the existing home palette (read home background/primary; do not invent a new brand). Increase title size vs body; keep empty-string behavior: still call `setTextViewText` with `""` as today.

If the widget is Glance, rebuild the Glance tree the same way: `Row { Image(mascot_idle_loop); Column { existing texts } }` with 16dp padding and rounded `GlanceModifier.background`.

If `idle_loop.webp` cannot be used as a looping widget source on the minSdk, use the first frame as a static `mascot_idle.webp` and keep the looping webp for the in-app fallback only.

Missing image: wrap `setImageViewResource` in the same null-safe path as today; if the resource is absent, skip setImage (keep previous) and do not crash.

- [ ] **Step 3: Do not call `setState` from the widget**

Provider updates data only. No WebView.

- [ ] **Step 4: Verify**

Install/resize widget: new look, same numbers/strings as before. Hide one field in debug data: empty as today. Home 3D still works.

- [ ] **Step 5: Commit**

```bash
git add src/main/res src
git commit -m "feat: restyle home widget around idle mascot loop"
```

---

### Task 10: Spec regression pass

**Files:** none new

**Interfaces:** none

- [ ] **Step 1: Assets**

glTF viewer: seven clips, skinned monster, simple ghost. `idle_loop.webp` readable at ~72dp.

- [ ] **Step 2: Home**

Idle on load; think/cook/success/error/celebrate/wave via real events or a debug button that calls `setState`. Latest-state-wins: fire think then error quickly → error. Leave home → paused.

- [ ] **Step 3: Fallback**

Temporarily rename assets `duo.glb`: home shows loop image, not blank. Restore file.

- [ ] **Step 4: Widget + regression**

Widget layout new, data same. No mascot WebView on non-home screens.

- [ ] **Step 5: Commit** (only if this pass required fixes)

```bash
git add -u
git commit -m "fix: mascot companion regression findings"
```

---

## Self-review

**Spec coverage**

| Spec item | Task |
| --- | --- |
| Restyle concept | 3 |
| Tripo/Meshy + monster rig, simple ghost | 4 |
| Blender glTF + idle webp | 5 |
| Viewer `setState`, loops, latest-wins | 2 (rules) + 6 (JS) |
| Home host, event map, home only | 7 |
| Pause off-screen | 8 |
| Fallback image, log once | 6 + 7 |
| Widget restyle, same data, no 3D | 9 |
| Testing list | 2, 7 tests + 10 |
| Skill stack | 3–6 |
| Source tree unknown until hydrate | 1 |

**Placeholder scan:** source-map keys are filled in Task 1 before later Android edits; `<PACKAGE>` and `ASSETS_DIR` are those keys, not open design TBDs.

**Type consistency:** clip strings and `MascotState.clipName()` match the spec table. JS `KNOWN` / `LOOPING` match `scripts/mascot_state.py`.
