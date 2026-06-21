package com.kanta.user.common;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends DomainException {
    public UnauthorizedException() {
        this("인증이 필요합니다.");
    }

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message, "UNAUTHORIZED");
    }
}
