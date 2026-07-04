# SherpaTTS Fork — Project Context for Claude Code

## What this project is

A fork of [woheller69/ttsEngine](https://github.com/woheller69/ttsEngine) — an Android Text-to-Speech engine built on sherpa-onnx (Next-gen Kaldi), using Piper/Coqui ONNX voice models. It registers as a system TTS provider so any Android app can use it.

The goal of this fork is to improve UX and add a text pre-processing pipeline (normalizer harness), while keeping the fork structured so that:

1. Upstream changes can be merged with minimal conflicts
2. Our additions could realistically be contributed back upstream as PRs
3. The architecture stays clean enough that a maintainer stumbling on this fork would find it readable and respectful

---

## Core architectural principle

**Never edit upstream files. Only extend them.**

Upstream files (`TtsService.kt`, `MainActivity.kt`, `LanguageListAdapter.kt`, `ModelDownloader.kt`, etc.) must remain byte-for-byte mergeable with woheller69/ttsEngine. Our code lives in a separate subpackage.

### Package structure

```
app/src/main/java/org/woheller69/ttsengine/
  
  # UPSTREAM FILES — do not modify
  TtsService.kt
  MainActivity.kt
  LanguageListAdapter.kt
  ModelDownloader.kt
  ... (everything from woheller69)

  # OUR FILES — separate subpackage
  praxis/
    normalizer/
      TextNormalizer.kt        ← orchestrator, calls the others
      PauseInjector.kt         ← punctuation → pause rules
      AbbreviationExpander.kt  ← Dr. → Doctor, API → A P I, etc.
      NumberNormalizer.kt      ← £1,200 → "twelve hundred pounds"
    ui/
      OnboardingFragment.kt    ← first-launch guided setup
      ModelCardView.kt         ← richer model list item
    theme/
      PraxisTheme.kt           ← Material3 + dark/AMOLED theme
    PraxisTtsService.kt        ← subclasses TtsService, injects normalizer
```

---

## The normalizer harness

This is the most important piece. It sits between the Android TTS API callback and sherpa-onnx synthesis:

```
Android TTS API call
  → onSynthesizeText() receives raw text
  → TextNormalizer.normalize(text)   ← our layer
  → sherpa-onnx synthesizer
  → audio output
```

`PraxisTtsService` subclasses upstream's `TtsService` and overrides `onSynthesizeText()` to inject the normalizer before calling `super` or the upstream synthesis method.

### Key normalizations (discovered empirically against Piper models)

- **Pause injection**: `. ` → `.. ` — Piper models produce more natural sentence pauses with double stops
- **Em dash**: `—` or ` - ` → `, ` — treated as a spoken comma pause
- **Semicolons**: `;` → `. ` — treated as sentence boundary
- **Abbreviations**: `Dr.` → `Doctor`, `Mr.` → `Mister`, `etc.` → `etcetera`
- **Acronyms**: `API`, `URL`, `WPP` → `A P I`, `U R L`, `W P P` (spaced for letter-by-letter reading)
- **Numbers/currency**: `£1,200` → `twelve hundred pounds`, `10am` → `10 a.m.`
- **ISO dates**: `2026-07-04` → `fourth of July twenty twenty six`
- **Paragraph breaks**: `\n\n` → insert sentence boundary marker

### TextNormalizer design rules

- Pure Kotlin, zero Android dependencies — must be testable as plain JVM unit tests
- Single entry point: `TextNormalizer.normalize(input: String): String`
- Each concern is a separate private extension function or helper class
- Rules are data-driven where possible (abbreviation map as a `Map<String, String>`) so they can be extended without logic changes
- Designed to be extractable as a standalone PR back to upstream

---

## Conflict surface with upstream

| File | Owner | Conflict risk |
|---|---|---|
| `TtsService.kt` and all upstream `.kt` files | Upstream only | None — we never touch these |
| `AndroidManifest.xml` | Both | One line: service registration swapped to `PraxisTtsService` |
| `build.gradle` | Both | Dependency block only if we add libraries |
| `praxis/**` | Us only | None |

When merging upstream:
```bash
git fetch upstream
git merge upstream/master
# Resolve only AndroidManifest.xml and build.gradle — nothing else should conflict
```

---

## UX improvement priorities

### Tier 1 — Quick wins
- [ ] Onboarding card on first launch (explains: what a TTS engine is, how to activate in Android Settings)
- [ ] Show model file size + quality tier before download starts
- [ ] AMOLED/dark theme (Material3 dynamic colour)
- [ ] Storage used indicator in Manage Languages
- [ ] Confirmation dialog before model delete

### Tier 2 — Medium effort
- [ ] Voice preview (short sample audio clip) before full model download
- [ ] In-app speech rate/pitch sliders that write back to TTS service (remove "go to Android settings" redirect)
- [ ] Model card UI (name, language, size, quality badge) instead of plain list rows
- [ ] Active voice indicator showing which model the system currently has selected
- [ ] Filter chips on language list (Piper vs Coqui, size, quality)

### Tier 3 — Features
- [ ] Simple pronunciation overrides (word → phoneme mapping file)
- [ ] Multiple voices per language with a picker
- [ ] Favourites / recently used voices

---

## Tech stack

- **Language**: Kotlin (stay with Kotlin — no reason to fight the platform for UX work)
- **UI**: Migrate screens to Jetpack Compose as we touch them. Do not rewrite all at once — do it screen by screen.
- **Theme**: Material3 with dynamic colour support
- **Build**: Gradle, `compileSdk 34`, `minSdk 26`
- **TTS core**: sherpa-onnx (pre-built `.so` files shipped in the repo — no native compilation needed)
- **Testing**: JUnit for `TextNormalizer` (no Android needed). Instrumented tests for UI flows.

---

## Git remotes

```bash
git remote add upstream https://github.com/woheller69/ttsEngine
git remote add origin https://github.com/YOUR_USERNAME/ttsEngine
```

Pull upstream periodically:
```bash
git fetch upstream
git merge upstream/master
```

---

## Contribution back to upstream

If a piece of our work is clean enough to PR back:
- `TextNormalizer.kt` and its tests are the first candidate — zero dependencies, self-contained
- Keep normalizer files free of any imports from the rest of `praxis/`
- Write unit tests before opening a PR — a maintainer is far more likely to merge tested code
- Open small, single-purpose PRs — not "here is everything we built"

---

## What we are NOT doing

- No Rust rewrite (would be a different project; UX improvements don't need it)
- No full app rewrite — extend, don't replace
- No modifications to sherpa-onnx bindings or model loading logic
- No changes to upstream files, even small ones — use subclassing/wrapping instead
