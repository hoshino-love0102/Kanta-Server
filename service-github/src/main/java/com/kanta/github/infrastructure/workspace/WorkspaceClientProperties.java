package com.kanta.github.infrastructure.workspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceClientProperties {
    private final String baseUrl;

    public WorkspaceClientProperties(@Value("${kanta.workspace.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }
}
