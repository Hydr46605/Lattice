package dev.beryl.lattice.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class UpdateVersions {
    private UpdateVersions() {
    }

    static String normalize(String version) {
        if (version == null) {
            return "";
        }
        String value = version.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        return value;
    }

    static int compare(String left, String right) {
        VersionParts leftParts = VersionParts.parse(left);
        VersionParts rightParts = VersionParts.parse(right);
        int max = Math.max(leftParts.numbers().size(), rightParts.numbers().size());
        for (int index = 0; index < max; index++) {
            int leftNumber = index < leftParts.numbers().size() ? leftParts.numbers().get(index) : 0;
            int rightNumber = index < rightParts.numbers().size() ? rightParts.numbers().get(index) : 0;
            int comparison = Integer.compare(leftNumber, rightNumber);
            if (comparison != 0) {
                return comparison;
            }
        }
        if (leftParts.qualifier().isBlank() && !rightParts.qualifier().isBlank()) {
            return 1;
        }
        if (!leftParts.qualifier().isBlank() && rightParts.qualifier().isBlank()) {
            return -1;
        }
        return leftParts.qualifier().compareTo(rightParts.qualifier());
    }

    private record VersionParts(List<Integer> numbers, String qualifier) {
        static VersionParts parse(String version) {
            String normalized = normalize(version).toLowerCase(Locale.ROOT);
            String withoutBuild = normalized.split("\\+", 2)[0];
            String[] mainAndQualifier = withoutBuild.split("-", 2);
            String main = mainAndQualifier[0];
            String qualifier = mainAndQualifier.length > 1 ? mainAndQualifier[1] : "";

            List<Integer> numbers = new ArrayList<>();
            for (String token : main.split("\\.")) {
                if (token.isBlank()) {
                    continue;
                }
                try {
                    numbers.add(Integer.parseInt(token));
                } catch (NumberFormatException exception) {
                    qualifier = qualifier.isBlank() ? token : token + "-" + qualifier;
                    break;
                }
            }
            return new VersionParts(List.copyOf(numbers), qualifier);
        }
    }
}
