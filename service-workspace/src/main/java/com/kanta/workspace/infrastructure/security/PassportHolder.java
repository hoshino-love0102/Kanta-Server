package com.kanta.workspace.infrastructure.security;

import com.kanta.workspace.common.UnauthorizedException;

public final class PassportHolder {
    private static final ThreadLocal<Passport> HOLDER = new ThreadLocal<>();

    private PassportHolder() {
    }

    public static Passport current() {
        var passport = HOLDER.get();
        if (passport == null) {
            throw new UnauthorizedException();
        }
        return passport;
    }

    public static void set(Passport passport) {
        HOLDER.set(passport);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
