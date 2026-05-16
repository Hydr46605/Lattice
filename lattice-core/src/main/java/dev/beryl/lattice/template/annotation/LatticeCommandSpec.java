package dev.beryl.lattice.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LatticeCommandSpec {
    String name();

    String[] aliases() default {};

    String permission() default "";
}

