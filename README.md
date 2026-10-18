# Philiox Axiom Addons

Fabric client addon for [Axiom](https://axiom.moulberry.com) on Minecraft 1.21.11 (Axiom 6.1.3).

- Extra shapes in Axiom's Shape Tool, with optional smoothing using stairs, slabs and walls
- Philiox Modern GUI theme
- Tools: tree generator, bridge, ship builder, ocean waves, ocean painter, weathering, ruins,
  gradient fill, colour palette, block displays
- Block filter for Nexo custom blocks, a blacklist and full blocks only (also for Autoshade)

## Building

Axiom is closed source and not on any Maven repository, so its jars are not part of this repo.
Put these files into `libs/` before building:

| File | Source |
|------|--------|
| `axiom-6.1.3.jar` | the Axiom mod jar |
| `axiomclientapi-6.1.3.jar` | `META-INF/jars/` inside the Axiom jar |
| `mixinconstraints-1.0.9.jar` | `META-INF/jars/` inside the Axiom jar |
| `lattice-1.3.0.jar` | `META-INF/jars/` inside the Axiom jar |

Then build with Java 21:

```
./gradlew build
```

The mod jar ends up in `build/libs/`.
