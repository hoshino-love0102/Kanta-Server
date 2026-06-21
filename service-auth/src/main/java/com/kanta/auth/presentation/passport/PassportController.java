package com.kanta.auth.presentation.passport;

import com.kanta.auth.application.passport.PassportExchangeUseCase;
import com.kanta.auth.common.ApiResponse;
import com.kanta.auth.common.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PassportController {
    private final PassportExchangeUseCase passportExchangeUseCase;

    public PassportController(PassportExchangeUseCase passportExchangeUseCase) {
        this.passportExchangeUseCase = passportExchangeUseCase;
    }

    @PostMapping("/passport")
    public ApiResponse<PassportResponse> exchange(@RequestHeader("Authorization") String authorizationHeader) {
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "INVALID_TOKEN");
        }

        var accessToken = authorizationHeader.substring("Bearer ".length());
        var passport = passportExchangeUseCase.exchange(accessToken);
        return ApiResponse.ok(new PassportResponse(passport));
    }
}
