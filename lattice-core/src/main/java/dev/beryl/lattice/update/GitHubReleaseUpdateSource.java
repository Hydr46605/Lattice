package dev.beryl.lattice.update;

import dev.beryl.lattice.util.Preconditions;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record GitHubReleaseUpdateSource(
        String owner,
        String repository,
        String currentVersion,
        URI apiBaseUri
) implements UpdateSource {
    private static final URI DEFAULT_API_BASE_URI = URI.create("https://api.github.com/");

    public GitHubReleaseUpdateSource {
        owner = Preconditions.requireText(owner, "owner");
        repository = Preconditions.requireText(repository, "repository");
        currentVersion = Preconditions.requireText(currentVersion, "currentVersion");
        apiBaseUri = apiBaseUri == null ? DEFAULT_API_BASE_URI : apiBaseUri;
    }

    public static GitHubReleaseUpdateSource of(String owner, String repository, String currentVersion) {
        return builder(owner, repository, currentVersion).build();
    }

    public static Builder builder(String owner, String repository, String currentVersion) {
        return new Builder(owner, repository, currentVersion);
    }

    @Override
    public URI latestReleaseUri() {
        URI base = baseWithTrailingSlash(apiBaseUri);
        return base.resolve("repos/" + encodePath(owner) + "/" + encodePath(repository) + "/releases/latest");
    }

    private static URI baseWithTrailingSlash(URI uri) {
        String value = uri.toString();
        if (value.endsWith("/")) {
            return uri;
        }
        return URI.create(value + "/");
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static final class Builder {
        private final String owner;
        private final String repository;
        private final String currentVersion;
        private URI apiBaseUri = DEFAULT_API_BASE_URI;

        private Builder(String owner, String repository, String currentVersion) {
            this.owner = owner;
            this.repository = repository;
            this.currentVersion = currentVersion;
        }

        public Builder apiBaseUri(URI apiBaseUri) {
            this.apiBaseUri = Preconditions.requireNonNull(apiBaseUri, "apiBaseUri");
            return this;
        }

        public GitHubReleaseUpdateSource build() {
            return new GitHubReleaseUpdateSource(owner, repository, currentVersion, apiBaseUri);
        }
    }
}
