# Forge icon set

Every icon here is hand-traced geometry compiled into an `ImageVector` — no drawable XML, no
runtime SVG parsing. That buys tintability and zero inflation cost, but it also means nothing
about a glyph's *shape* is checked by the compiler. Consistency comes from the keylines below
and from the golden images; there is no third safety net.

## Grid and keylines

The set follows Google's Material system-icon metrics. All measurements are viewport units,
where the viewport is 24×24 (`IconViewportSize`) and ships at 24dp (`IconDefaultSize`).

| | Spec |
|---|---|
| Viewport / trim area | 24 × 24 |
| Live area | 20 × 20, inset 2 on every side (x and y run 2..22) |
| Padding | 2 minimum — nothing but a deliberate bleed reaches the trim edge |
| Stroke weight | 2 nominal (see `Tune.kt`'s `THICKNESS`) |
| Corner radius | 2 on exterior corners, unless the shape argues otherwise |

Within the live area, size is set by **how solid the glyph is**, not by the live area alone —
this is the part that gets missed, and the reason two icons can both "fill the box" and still
look a size apart. Ink area drives perceived size, so a filled mass has to sit smaller than an
open outline to read the same.

**Solid, closed forms** take Google's keyline shapes:

| Keyline shape        | Size         | In this set                                       |
|----------------------|--------------|---------------------------------------------------|
| Square               | 18 × 18      | `PictureMode`, `CameraSettings`                   |
| Circle               | 20 × 20      | — (the extra 2 offsets a circle's optical shrink) |
| Vertical rectangle   | 16 w × 20 h  | —                                                 |
| Horizontal rectangle | 20 w × 16 h  | `VideoMode`                                       |

A solid square glyph is **18, not 20**. Only circles earn the full 20.

**Open outline forms** — frames, rules, arcs, anything whose extremes are thin strokes with gaps
between them — are drawn to the live-area edge instead, 20 × 20: `Debug`, `FramePerson`, `Tune`,
and `CameraFlip` at 20 wide by its natural height. Held to the 18 square these read a size small,
because only a few stroke-ends ever reach the bounding box.

That split is this set's convention rather than something Google spells out; Google's keylines
assume a solid dominant form, and its own frame and rule glyphs likewise run to the live area.

Two things to hold loosely. Keylines are a starting point, not a verdict — bounding box is only
a proxy for optical size, so trust the rendered comparison over the measurement. And the numbers
describe the **ink**, not the path data: a glyph traced at other proportions should be fitted
with a `group` transform (see `Debug.kt`) rather than having every coordinate rewritten.

All glyphs centre on exactly (12, 12).

## Anatomy of an icon file

One glyph per file, filename matching the property (detekt's `MatchingDeclarationName`
enforces this). Copy `FramePerson.kt` for a plain traced path, or `Debug.kt` if you need a
fitting transform. Each file holds, in order:

1. `val Icons.Name: ImageVector` — a `get()` that returns the cache if warm, otherwise builds.
2. `private var nameCache: ImageVector?` — file-private, lowercase-initial, `.also { }`-assigned.
3. Any private geometry helpers and `const` dimensions, named for what they mean.
4. `@Preview` composable rendering through `ForgeTheme` at `dimensions.size12x`.

The path is filled `SolidColor(Color.Black)`; `Icon`'s `tint` replaces it at the call site.
Comment the *geometry* — winding direction, why a clip exists, how a shape was fitted — not
the Compose calls, which speak for themselves.

## Adding a glyph

1. **Trace it.** Convert the source SVG's path data to `PathBuilder` calls. Watch for a leading
   relative `m`, which SVG treats as absolute; and remember every coordinate scales with the
   viewBox (a 12×12 source doubles onto 24×24).
2. **Size it to its keyline.** Measure the traced ink, not the viewBox — art is usually inset
   inside its own box. If the trace doesn't land on the keyline, wrap it in a `group` with
   `pivotX`/`pivotY` at the viewport centre and a scale factor, as `Debug.kt` does.
3. **Write the file** per the anatomy above.
4. **Register it in the golden test.** Add a row to `icons()` in `ForgeIconGoldenTest`. An icon
   absent from that list ships unguarded — nothing else will ever render it.
5. **Record and eyeball the PNG.** Recording is not a formality: it is the only check that the
   winding, clips and fit are right.
6. **Commit the snapshot** alongside the source file.

```bash
./gradlew :lib-design-system:recordPaparazziDebug   # writes src/test/snapshots/images/*.png
./gradlew :lib-design-system:ktlintFormat           # fixes wrapping, then:
./gradlew :lib-design-system:ktlintCheck :lib-design-system:detekt
./gradlew :lib-design-system:compileDebugKotlin
```

## Updating a glyph

Edit the geometry, re-record, and **review the image diff before committing it** — re-recording
overwrites the golden, so an unreviewed record silently blesses whatever you just broke. To see
the failure first, verify against the old golden, then record:

```bash
./gradlew :lib-design-system:verifyPaparazziDebug   # fails, writes a delta to build/paparazzi/failures/debug
./gradlew :lib-design-system:recordPaparazziDebug   # bless it once the diff looks right
```

## Removing a glyph

1. Delete the icon file.
2. Delete its row from `icons()` in `ForgeIconGoldenTest`.
3. Delete its snapshot:
   `src/test/snapshots/images/app.honguyen.forge.designsystem.theme.icons_ForgeIconGoldenTest_matchesGolden[Name]_name.png`
4. `./gradlew :lib-design-system:compileDebugKotlin` to catch call sites left behind.

A stale PNG is not caught by anything — Paparazzi verifies the snapshots it renders, not the
directory contents — so step 3 is on you.