# Build And Release

This guide is for maintainers publishing Lattice artifacts.

## Local Build

Use the checked-in Gradle wrapper:

```bash
./gradlew build
```

If the shared Gradle cache on the machine is unhealthy, use an isolated Gradle home:

```bash
./gradlew --gradle-user-home .gradle-user-home --no-daemon build
```

Verify Maven publications locally:

```bash
./gradlew publishToMavenLocal
```

Build the standalone Paper/Folia jar used for release attachments and Modrinth:

```bash
./gradlew :lattice-paper:standaloneJar
```

## GitHub Packages

The Gradle build publishes all Maven modules to GitHub Packages:

- `dev.beryl:lattice-api:0.8.0`
- `dev.beryl:lattice-core:0.8.0`
- `dev.beryl:lattice-paper:0.8.0`

Publication uses the `GitHubPackages` Maven repository:

```text
https://maven.pkg.github.com/hydr46605/Lattice
```

GitHub Actions publishes with `GITHUB_ACTOR` and `GITHUB_TOKEN`. The release workflow needs:

```yaml
permissions:
  contents: write
  packages: write
```

Manual local publishing requires a token with package write access:

```bash
GITHUB_ACTOR=YourGitHubUsername \
GITHUB_TOKEN=YourClassicTokenWithWritePackages \
./gradlew publish
```

## GitHub Release

The `GitHub Release` workflow builds, publishes Maven packages, and creates or updates a GitHub Release with:

- `lattice-api` jar
- `lattice-api` sources jar
- `lattice-api` Javadoc jar
- `lattice-core` jar
- `lattice-core` sources jar
- `lattice-core` Javadoc jar
- `lattice-paper` jar
- `lattice-paper` sources jar
- `lattice-paper` Javadoc jar
- `lattice-paper` standalone jar

Run it manually for a controlled release:

```bash
gh workflow run "GitHub Release" \
  --repo Hydr46605/Lattice \
  -f version=0.8.0 \
  -f prerelease=true
```

Or publish by pushing a version tag:

```bash
git tag v0.8.0
git push origin v0.8.0
```

The release workflow checks that the requested version matches `latticeVersion` in `gradle.properties`.

## Modrinth Release

The `Modrinth Release` workflow publishes the standalone `lattice-paper` jar to Modrinth. It requires:

- repository variable `MODRINTH_PROJECT_ID`
- repository secret `MODRINTH_TOKEN`

Run it manually when the Modrinth version should be published separately:

```bash
gh workflow run "Modrinth Release" \
  --repo Hydr46605/Lattice \
  -f version=0.8.0 \
  -f version_type=beta
```

The workflow should be run only once for each Modrinth version.

Modrinth changelogs are read from `docs/release-notes/<version>.md`. Add that file before running either release workflow.
