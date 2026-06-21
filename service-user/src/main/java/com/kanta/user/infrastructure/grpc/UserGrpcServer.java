package com.kanta.user.infrastructure.grpc;

import com.kanta.user.application.user.UserService;
import com.kanta.user.grpc.UpsertUserRequest;
import com.kanta.user.grpc.UpsertUserResponse;
import com.kanta.user.grpc.UserInternalServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class UserGrpcServer extends UserInternalServiceGrpc.UserInternalServiceImplBase {
    private final UserService userService;

    public UserGrpcServer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void upsertUser(UpsertUserRequest request, StreamObserver<UpsertUserResponse> responseObserver) {
        var result = userService.upsert(request.getEmail(), request.getDisplayName());

        responseObserver.onNext(
            UpsertUserResponse.newBuilder()
                .setUserId(result.userId().toString())
                .setEmail(result.email())
                .setDisplayName(result.displayName())
                .setRole(result.role())
                .build()
        );
        responseObserver.onCompleted();
    }
}
