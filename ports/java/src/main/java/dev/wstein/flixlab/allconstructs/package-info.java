/**
 * Comparative-reference Java port of {@code src/AllConstructs.flix}. See {@code docs/ports.md} at
 * the repository root for what this is, what it deliberately does not measure, and the full
 * per-language mapping table.
 *
 * <p>Two constructs have no equivalent in Java at all, stated here rather than faked with an
 * unrelated substitute:
 *
 * <ul>
 *   <li><b>Type aliases</b> (Flix section 2) -- Java has no type-level alias mechanism; a {@code
 *       Map<String, Integer>} is spelled out every time.
 *   <li><b>Flix's {@code unsafe IO} block</b> (section 15) -- it discharges a static effect-typing
 *       obligation that Java method signatures never carry in the first place, so there is nothing
 *       to unsafely bypass.
 * </ul>
 */
package dev.wstein.flixlab.allconstructs;
