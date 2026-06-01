package dev.beryl.lattice.command;

@FunctionalInterface
public interface CommandArgumentParser<T> {
    T parse(String input) throws Exception;
}
