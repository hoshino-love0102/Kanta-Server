package com.kanta.websockethub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kanta.websockethub.auth.AuthPassportClient;
import com.kanta.websockethub.auth.AuthPassportClient.ExchangedPassport;
import com.kanta.websockethub.auth.KanbanBoardClient;
import com.kanta.websockethub.auth.WorkspaceMembershipClient;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

class StompAuthChannelInterceptorTest {

    private AuthPassportClient authPassportClient;
    private KanbanBoardClient kanbanBoardClient;
    private WorkspaceMembershipClient workspaceMembershipClient;
    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        authPassportClient = mock(AuthPassportClient.class);
        kanbanBoardClient = mock(KanbanBoardClient.class);
        workspaceMembershipClient = mock(WorkspaceMembershipClient.class);
        interceptor = new StompAuthChannelInterceptor(authPassportClient, kanbanBoardClient, workspaceMembershipClient);
    }

    @Test
    void connect_없이_Authorization_헤더가_없으면_거부한다() {
        var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>());
        var message = org.springframework.messaging.support.MessageBuilder.createMessage(
            new byte[0], accessor.getMessageHeaders()
        );

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connect_유효한_토큰이면_passport를_세션에_저장한다() {
        var passport = new Passport("user-1", "이우진", "MEMBER");
        when(authPassportClient.exchange("valid-token")).thenReturn(new ExchangedPassport("encoded-passport", passport));

        var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        var sessionAttributes = new HashMap<String, Object>();
        accessor.setSessionAttributes(sessionAttributes);
        var message = org.springframework.messaging.support.MessageBuilder.createMessage(
            new byte[0], accessor.getMessageHeaders()
        );

        interceptor.preSend(message, null);

        assertThat(sessionAttributes.get(StompAuthChannelInterceptor.PASSPORT_VALUE_ATTRIBUTE)).isEqualTo("encoded-passport");
        assertThat(sessionAttributes.get(StompAuthChannelInterceptor.PASSPORT_ATTRIBUTE)).isEqualTo(passport);
    }

    @Test
    void subscribe_인증되지_않은_세션이면_거부한다() {
        var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/boards/" + UUID.randomUUID());
        accessor.setSessionAttributes(new HashMap<>());
        var message = org.springframework.messaging.support.MessageBuilder.createMessage(
            new byte[0], accessor.getMessageHeaders()
        );

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void subscribe_허용되지_않은_destination이면_거부한다() {
        var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/anything-else");
        var sessionAttributes = new HashMap<String, Object>();
        sessionAttributes.put(StompAuthChannelInterceptor.PASSPORT_VALUE_ATTRIBUTE, "encoded-passport");
        accessor.setSessionAttributes(sessionAttributes);
        var message = org.springframework.messaging.support.MessageBuilder.createMessage(
            new byte[0], accessor.getMessageHeaders()
        );

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subscribe_워크스페이스_멤버가_아니면_거부한다() {
        var boardId = UUID.randomUUID();
        var workspaceId = UUID.randomUUID();
        when(kanbanBoardClient.findWorkspaceId(boardId, "encoded-passport")).thenReturn(workspaceId);
        org.mockito.Mockito.doThrow(new com.kanta.websockethub.auth.SubscriptionDeniedException("거부", null))
            .when(workspaceMembershipClient).requireMember(workspaceId, "encoded-passport");

        var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/boards/" + boardId);
        var sessionAttributes = new HashMap<String, Object>();
        sessionAttributes.put(StompAuthChannelInterceptor.PASSPORT_VALUE_ATTRIBUTE, "encoded-passport");
        accessor.setSessionAttributes(sessionAttributes);
        var message = org.springframework.messaging.support.MessageBuilder.createMessage(
            new byte[0], accessor.getMessageHeaders()
        );

        assertThatThrownBy(() -> interceptor.preSend(message, null))
            .isInstanceOf(com.kanta.websockethub.auth.SubscriptionDeniedException.class);
    }
}
