package com.mycompany.tutorhub_enterprise.client.search;

@FunctionalInterface
public interface SearchAction {
    void execute();

    static SearchAction noop() {
        return () -> {
        };
    }
}
