package com.kanta.websockethub.security;

import com.kanta.websockethub.auth.AuthPassportClient;
import com.kanta.websockethub.auth.KanbanBoardClient;
import com.kanta.websockethub.auth.WorkspaceMembershipClient;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    public static final String PASSPORT_VALUE_ATTRIBUTE = "passportValue";
    public static final String PASSPORT_ATTRIBUTE = "passport";

    private static final Pattern BOARD_TOPIC_PATTERN = Pattern.compile("^/topic/boards/([0-9a-fA-F-]{36})$");

    private final AuthPassportClient authPassportClient;
    private final KanbanBoardClient kanbanBoardClient;
    private final WorkspaceMembershipClient workspaceMembershipClient;

    public StompAuthChannelInterceptor(
        AuthPassportClient authPassportClient,
        KanbanBoardClient kanbanBoardClient,
        WorkspaceMembershipClient workspaceMembershipClient
    ) {
        this.authPassportClient = authPassportClient;
        this.kanbanBoardClient = kanbanBoardClient;
        this.workspaceMembershipClient = workspaceMembershipClient;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        var authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더가 필요합니다.");
        }

        var accessToken = authorization.substring("Bearer ".length());
        var exchanged = authPassportClient.exchange(accessToken);

        var sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalStateException("STOMP 세션 attribute를 사용할 수 없습니다.");
        }
        sessionAttributes.put(PASSPORT_VALUE_ATTRIBUTE, exchanged.rawValue());
        sessionAttributes.put(PASSPORT_ATTRIBUTE, exchanged.passport());
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        var sessionAttributes = accessor.getSessionAttributes();
        var passportValue = sessionAttributes == null ? null : (String) sessionAttributes.get(PASSPORT_VALUE_ATTRIBUTE);
        if (passportValue == null) {
            throw new IllegalStateException("인증되지 않은 세션입니다.");
        }

        var destination = accessor.getDestination();
        var matcher = BOARD_TOPIC_PATTERN.matcher(destination == null ? "" : destination);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("허용되지 않은 destination입니다: " + destination);
        }

        var boardId = UUID.fromString(matcher.group(1));
        var workspaceId = kanbanBoardClient.findWorkspaceId(boardId, passportValue);
        workspaceMembershipClient.requireMember(workspaceId, passportValue);
    }
}
