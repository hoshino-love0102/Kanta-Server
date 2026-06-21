package com.kanta.user.application.user;

import com.kanta.user.application.outbox.OutboxEventWriter;
import com.kanta.user.domain.user.entity.User;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventWriterDelegate {
    private static final String AGGREGATE_TYPE = "USER";

    private final OutboxEventWriter outboxEventWriter;

    public OutboxEventWriterDelegate(OutboxEventWriter outboxEventWriter) {
        this.outboxEventWriter = outboxEventWriter;
    }

    public void appendUserCreated(User user) {
        outboxEventWriter.append(AGGREGATE_TYPE, user.getId(), "user.created", payload(user));
    }

    public void appendUserUpdated(User user) {
        outboxEventWriter.append(AGGREGATE_TYPE, user.getId(), "user.updated", payload(user));
    }

    private Map<String, Object> payload(User user) {
        return Map.of(
            "userId", user.getId().toString(),
            "email", user.getEmail(),
            "displayName", user.getDisplayName(),
            "role", user.getRole()
        );
    }
}
