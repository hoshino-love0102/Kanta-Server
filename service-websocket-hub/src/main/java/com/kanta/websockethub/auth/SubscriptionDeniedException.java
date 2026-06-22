package com.kanta.websockethub.auth;

public class SubscriptionDeniedException extends RuntimeException {
    public SubscriptionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
