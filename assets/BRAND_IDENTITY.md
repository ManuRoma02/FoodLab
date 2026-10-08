# 🧪 FoodLab — Brand Identity & Design System

## 1. Vision & Archetype
**FoodLab** unisce la precisione analitica della biochimica nutrizionale ("Lab") alla gioia e all'empatia dell'abitudine sostenibile ("Food & Companion"). 
L'archetipo visivo è il **Bio-Core Mascot Duo**: uno slime energetico smeraldo bio-luminescente e il suo spirito guida etereo.

---

## 2. Palette Cromatica Ufficiale

### Brand Core
| Token | HEX | RGB | Utilizzo |
| :--- | :--- | :--- | :--- |
| **Primary Green (Bio-Core)** | `#22C55E` | `rgb(34, 197, 94)` | Colore primario del brand, target completati, proteine, stato attivo |
| **Primary Hover / Glow** | `#4ADE80` | `rgb(74, 222, 128)` | Specular highlights, bordi radianti, glow al tocco |
| **Primary Deep** | `#15803D` | `rgb(21, 128, 61)` | Ombre vettoriali, badge pressati, contrasto accessibile su sfondi chiari |

### Nutrition Macro Triad
| Macro | HEX | Descrizione |
| :--- | :--- | :--- |
| **Proteine** | `#22C55E` | Smeraldo vitale (costruzione e recupero muscolare) |
| **Carboidrati** | `#F59E0B` | Ambra solare (energia dinamica e glicogeno) |
| **Grassi** | `#06B6D4` / `#38BDF8` | Ciano / Azzurro limpido (funzioni ormonali e assorbimento) |

### Superfici & Background (Dark First — AMOLED Ready)
| Token | HEX | Utilizzo |
| :--- | :--- | :--- |
| **Obsidian Base** | `#0A0E13` | Sfondo assoluto display (risparmio energetico AMOLED) |
| **Surface Midnight** | `#11161D` | Sfondo schede primarie e contenitori |
| **Surface Frosted Glass** | `rgba(26, 34, 44, 0.82)` | `BottomNavBar`, card flottanti, dialoghi traslucidi con blur |
| **Border Radiant Highlight** | `rgba(255, 255, 255, 0.18)` | Riflesso luminoso superiore (Apple rim-light) |

### Light Mode Counterpart
| Token | HEX | Utilizzo |
| :--- | :--- | :--- |
| **Canvas Light** | `#F8FAFC` | Sfondo pagina chiaro |
| **Surface White** | `#FFFFFF` | Card elevate e dialoghi |
| **Text High-Contrast** | `#0F172A` | Tipografia titoli e numeri |

---

## 3. Tipografia & Tracking Ottico (Apple Typography HIG)
- **Display Headlines**: Tracking negativo (`-0.02em` / `-0.5sp`), peso Bold / Heavy per massimo impatto visivo compatto.
- **Micro-Labels & Caption (<12sp)**: Tracking positivo (`0.2sp`), peso SemiBold con spaziatura ottica per massima leggibilità a colpo d'occhio.

---

## 4. Specifiche App Icon & Mascheratura
- **Master Canvas**: 1024×1024 px quadrato pieno (nessun angolo pre-arrotondato, nessuna cornice).
- **Safe Zone (80% Inner Circle)**: Raggio centrale di 410px (su canvas 512px) o 820px (su canvas 1024px).
- **Themed / Tinted**: Sagoma monocromatica ad alto contrasto per Android 13+ e iOS 18 Control Center/Homescreen tints.
