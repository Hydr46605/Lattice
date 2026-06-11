package dev.beryl.lattice.update;

import dev.beryl.lattice.util.Preconditions;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public record UpdateRelease(
        String tagName,
        Optional<String> name,
        Optional<URI> htmlUrl,
        List<UpdateAsset> assets
) {
    public UpdateRelease {
        tagName = Preconditions.requireText(tagName, "tagName");
        name = name == null ? Optional.empty() : name.filter(value -> !value.isBlank());
        htmlUrl = htmlUrl == null ? Optional.empty() : htmlUrl;
        assets = assets == null ? List.of() : List.copyOf(assets);
    }

    public UpdateRelease(String tagName, String name, URI htmlUrl, List<UpdateAsset> assets) {
        this(tagName, Optional.ofNullable(name), Optional.ofNullable(htmlUrl), assets);
    }

    public String version() {
        return UpdateVersions.normalize(tagName);
    }
}
