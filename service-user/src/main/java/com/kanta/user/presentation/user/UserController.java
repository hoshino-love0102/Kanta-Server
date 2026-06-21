package com.kanta.user.presentation.user;

import com.kanta.user.application.user.UserService;
import com.kanta.user.common.ApiResponse;
import com.kanta.user.infrastructure.security.PassportHolder;
import com.kanta.user.infrastructure.security.UserAccess;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@UserAccess
@RestController
@RequestMapping("/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<?> getMe() {
        var userId = PassportHolder.current().requireUserId();
        return ApiResponse.ok(userService.getMe(userId));
    }

    @PatchMapping
    public ApiResponse<?> patchMe(@RequestBody PatchUserRequest request) {
        var userId = PassportHolder.current().requireUserId();
        return ApiResponse.ok(userService.patchMe(userId, request.displayName()));
    }
}
