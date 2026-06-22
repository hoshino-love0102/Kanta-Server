package com.kanta.github.infrastructure.kanban;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KanbanClientProperties {
    private final String baseUrl;

    public KanbanClientProperties(@Value("${kanta.kanban.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }
}
