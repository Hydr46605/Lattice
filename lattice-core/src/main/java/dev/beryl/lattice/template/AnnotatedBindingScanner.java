package dev.beryl.lattice.template;

import java.util.Collection;

public interface AnnotatedBindingScanner {
    Collection<Class<?>> scan(String basePackage);
}

