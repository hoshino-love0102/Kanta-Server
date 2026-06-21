package com.kanta.kanban.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserAccessInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        var requiresUser = handlerMethod.hasMethodAnnotation(UserAccess.class)
            || handlerMethod.getBeanType().isAnnotationPresent(UserAccess.class);

        if (requiresUser) {
            PassportHolder.current();
        }
        return true;
    }
}
