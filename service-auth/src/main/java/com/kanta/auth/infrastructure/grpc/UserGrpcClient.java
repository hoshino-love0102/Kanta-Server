package com.kanta.auth.infrastructure.grpc;

import com.kanta.user.grpc.UpsertUserRequest;
import com.kanta.user.grpc.UserInternalServiceGrpc;
import java.util.UUID;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class UserGrpcClient {
    @GrpcClient("service-user")
    private UserInternalServiceGrpc.UserInternalServiceBlockingStub stub;

    public UpsertedUser upsertUser(String email, String displayName) {
        var response = stub.upsertUser(
            UpsertUserRequest.newBuilder()
                .setEmail(email)
                .setDisplayName(displayName)
                .build()
        );

        return new UpsertedUser(
            UUID.fromString(response.getUserId()),
            response.getEmail(),
            response.getDisplayName(),
            response.getRole()
        );
    }

    public record UpsertedUser(UUID userId, String email, String displayName, String role) {
    }
}
