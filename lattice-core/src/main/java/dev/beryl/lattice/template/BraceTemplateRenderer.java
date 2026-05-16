package dev.beryl.lattice.template;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BraceTemplateRenderer {
    private static final Pattern PLACEHOLDER =
            Pattern.compile("(?<!\\\\)\\{\\{\\s*([A-Za-z0-9_.-]+)(?:\\|([^}]*))?\\s*}}");

    public boolean containsTemplate(String input) {
        return input != null && PLACEHOLDER.matcher(input).find();
    }

    public TemplateRenderResult render(String input, Map<String, String> variables) {
        return render(input, TemplateVariableResolver.of(variables));
    }

    public TemplateRenderResult render(String input, TemplateVariableResolver resolver) {
        String source = input == null ? "" : input;
        TemplateVariableResolver selected = resolver == null ? TemplateVariableResolver.empty() : resolver;
        Matcher matcher = PLACEHOLDER.matcher(source);
        StringBuilder output = new StringBuilder();
        Set<String> used = new LinkedHashSet<>();
        Set<String> unresolved = new LinkedHashSet<>();

        while (matcher.find()) {
            String key = matcher.group(1);
            String fallback = matcher.group(2);
            Optional<String> resolved = selected.resolve(key);
            String replacement = resolved.orElse(fallback);
            if (replacement == null) {
                unresolved.add(key);
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
            } else {
                used.add(key);
                matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(output);
        return new TemplateRenderResult(output.toString().replace("\\{{", "{{"), used, unresolved);
    }
}
