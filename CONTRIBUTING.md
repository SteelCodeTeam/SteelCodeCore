# Contributing to SteelCodeCore

Thanks for helping! Bug reports, docs fixes and pull requests are all welcome.

## Reporting bugs

Open an issue with the bug template. Always include the Minecraft, loader (Fabric/NeoForge),
SteelCodeCore and Architectury versions, and attach the full `latest.log` or crash report.

## Development setup

Requirements: JDK 21. Everything else is downloaded by Gradle.

```bash
./gradlew build               # both loaders
./gradlew :fabric:runClient   # or :neoforge:runClient
./gradlew buildDocs           # docs site into docs/ (python3 + docs-src/requirements.txt)
```

## Code guidelines

- Public API lives in `dev.steelcode.core.api` (and `content`). Everything in `impl` is internal and annotated
  `@ApiStatus.Internal`; keep new internals there.
- Every public class and method of the API needs Javadoc in English with an example when useful. The build
  runs Javadoc with doclint, so broken references fail it.
- Prefer builder methods that read well in a chain and return `this`; validate arguments early with clear
  messages.
- Generated resources must stay identical on both loaders: put loader-specific behaviour behind
  `CorePlatform.Hooks`.
- Write commit messages in the imperative ("Add crystal builder"), one topic per pull request.

## Documentation

Guide pages are Markdown files in `docs-src/pages/`. Do not edit `docs/`: it is generated.

## Releases (maintainers)

1. Move the `[Unreleased]` entries in `CHANGELOG.md` to a new `## [x.y.z] - date` section.
2. Tag and push: `git tag vx.y.z && git push origin vx.y.z`.
3. The release workflow publishes to the Codeberg Maven registry and creates the GitHub release.

## License

By contributing you agree that your contributions are licensed under the
[GNU LGPL v3.0 or later](COPYING.LESSER).
