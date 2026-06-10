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

## Maven Central

The primary public Maven publication target is Maven Central:

- `io.github.hydr46605:lattice-api:0.8.3`
- `io.github.hydr46605:lattice-core:0.8.3`
- `io.github.hydr46605:lattice-paper:0.8.3`

The `io.github.hydr46605` namespace must be available in the Sonatype Central Portal before the first Maven Central release. Central publication also requires Apache-2.0 license metadata and signed artifacts.

The release workflow needs these repository secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_IN_MEMORY_KEY`
- `SIGNING_IN_MEMORY_KEY_PASSWORD`
- optional `SIGNING_IN_MEMORY_KEY_ID`

Generate the in-memory signing key from the maintainer GPG key with:

```bash
gpg --export-secret-keys --armor <key-id>
```

The release workflow maps those secrets to the Gradle properties consumed by the Maven publishing plugin and runs:

```bash
./gradlew publishAndReleaseToMavenCentral
```

Central releases are immutable. If a release upload succeeds but validation fails, fix the project and release a new version instead of reusing the same version.

Sonatype can occasionally keep a deployment in the publishing state longer than the workflow waits. The exact timeout:

```text
Deployment validation timed out after 900s. Last known state: PUBLISHING
```

is treated as a non-fatal `publishing_timeout` result so the workflow can continue to the mirror and GitHub Release. After that happens, check the Central Portal before rerunning any Maven Central publish step for the same version.

## GitHub Packages Mirror

The Gradle build also publishes all Maven modules to GitHub Packages as a permanent authenticated mirror under the original coordinates:

- `dev.beryl:lattice-api:0.8.3`
- `dev.beryl:lattice-core:0.8.3`
- `dev.beryl:lattice-paper:0.8.3`

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

Manual local mirror publishing requires a token with package write access:

```bash
GITHUB_ACTOR=YourGitHubUsername \
GITHUB_TOKEN=YourClassicTokenWithWritePackages \
./gradlew publishGitHubPackagesMirror
```

## GitHub Release

The `GitHub Release` workflow builds the project, publishes the selected package targets, then creates or refreshes a GitHub Release with:

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

When `create_github_release=true`, the workflow requires `docs/release-notes/<version>.md` before publishing. The release notes are copied into the GitHub Release and get an artifacts section appended. The GitHub Packages mirror line is added only when the mirror publish succeeds in that run.

Run it manually for a controlled release:

```bash
gh workflow run "GitHub Release" \
  --repo Hydr46605/Lattice \
  -f version=0.8.3 \
  -f prerelease=false \
  -f publish_maven_central=true \
  -f publish_github_packages=true \
  -f create_github_release=true
```

Or publish by pushing a version tag:

```bash
git tag v0.8.3
git push origin v0.8.3
```

Manual runs default to `prerelease=false`. Tag pushes infer prerelease status from the version name: only suffixes labeled `alpha`, `beta`, `rc`, `snapshot`, or `dev`, with optional qualifiers, are marked as prereleases. A normal `v0.8.3` tag is a stable release.

The workflow checks that the requested version matches `latticeVersion` in `gradle.properties`. Stable GitHub Releases are marked as latest. Prereleases are marked as prereleases and are not promoted to latest.

The manual publish switches are for recovery, not for the normal first attempt:

- Keep `publish_maven_central=true`, `publish_github_packages=true`, and `create_github_release=true` for a normal release.
- Set `publish_maven_central=false` only when Maven Central already has the immutable version, including after the known `publishing_timeout` result or after a later step failed in a previous run.
- Set `publish_github_packages=false` when recovering after the mirror has already been published, or when the mirror should not be touched on the rerun. If a GitHub Release is created while this is false, the generated notes will not add the mirror line.
- Set `create_github_release=false` when repairing package publication without changing the GitHub Release page. Release notes are validated only when this switch is true.

After the workflow finishes, verify:

- the Actions run completed successfully, or only had the known Maven Central `publishing_timeout` warning;
- the `v0.8.3` GitHub Release points at the intended commit, has all expected jar attachments, and has the correct latest/prerelease state;
- the release notes include the expected changelog and artifact lines;
- Maven Central resolves `io.github.hydr46605:lattice-api:0.8.3`, `io.github.hydr46605:lattice-core:0.8.3`, and `io.github.hydr46605:lattice-paper:0.8.3`;
- GitHub Packages resolves the `dev.beryl` mirror coordinates if the mirror was published.

## Modrinth Release

The `Modrinth Release` workflow publishes the standalone `lattice-paper` jar to Modrinth. It requires:

- repository variable `MODRINTH_PROJECT_ID`
- repository secret `MODRINTH_TOKEN`

Run it manually when the Modrinth version should be published separately:

```bash
gh workflow run "Modrinth Release" \
  --repo Hydr46605/Lattice \
  -f version=0.8.3 \
  -f version_type=beta
```

The workflow should be run only once for each Modrinth version.

Modrinth changelogs are read from `docs/release-notes/<version>.md`. Add that file before running either release workflow.

## Modrinth Project Metadata

The Modrinth project summary and long description are tracked in:

- `docs/modrinth-summary.txt`
- `docs/modrinth-description.md`

After editing those files, sync the live Modrinth project page with:

```bash
gh workflow run "Modrinth Project Metadata" \
  --repo Hydr46605/Lattice
```

The workflow uses the same `MODRINTH_PROJECT_ID` repository variable and `MODRINTH_TOKEN` repository secret as the release workflow, then reads the project back from Modrinth to verify the update.

The token used for metadata sync must include Modrinth `PROJECT_WRITE` scope for the Lattice project. A token that can publish versions may still fail this workflow if it was created only for release uploads.
