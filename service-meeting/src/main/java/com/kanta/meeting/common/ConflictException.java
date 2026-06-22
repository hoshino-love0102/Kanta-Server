package com.kanta.meeting.common;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {
    public ConflictException(String message, String errorCode) {
        super(HttpStatus.CONFLICT, message, errorCode);
    }
}
