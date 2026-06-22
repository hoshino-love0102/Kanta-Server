package com.kanta.github.infrastructure.workspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.kanta.github.domain.workspace.RepoBoardMapping;
import com.kanta.github.domain.workspace.WorkspaceRepoResolver;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestWorkspaceRepoResolver implements WorkspaceRepoResolver {
    private final RestClient restClient;

    public RestWorkspaceRepoResolver(RestClient.Builder restClientBuilder, WorkspaceClientProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Optional<RepoBoardMapping> resolve(String githubRepo) {
        try {
            var response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/repo-mappings/lookup")
                    .queryParam("githubRepo", githubRepo)
                    .build())
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, clientResponse) -> {
                })
                .body(JsonNode.class);

            if (response == null) {
                return Optional.empty();
            }
            var data = response.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return Optional.empty();
            }
            var workspaceId = UUID.fromString(data.path("workspaceId").asText());
            var boardId = UUID.fromString(data.path("boardId").asText());
            return Optional.of(new RepoBoardMapping(workspaceId, boardId));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
