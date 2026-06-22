package com.kanta.websockethub.auth;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WorkspaceMembershipClient {
    private final RestClient restClient;

    public WorkspaceMembershipClient(@Value("${kanta.client.workspace-service-url}") String workspaceServiceUrl) {
        this.restClient = RestClient.create(workspaceServiceUrl);
    }

    public void requireMember(UUID workspaceId, String passportValue) {
        try {
            restClient.get()
                .uri("/workspaces/{workspaceId}/members?size=1", workspaceId)
                .header("X-User-Passport", passportValue)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatusCode.valueOf(403)) {
                throw new SubscriptionDeniedException("워크스페이스 멤버가 아닙니다.", exception);
            }
            throw new SubscriptionDeniedException("워크스페이스 멤버십을 확인할 수 없습니다.", exception);
        }
    }
}
