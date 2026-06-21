package com.kanta.auth.infrastructure.grpc;

import com.kanta.auth.domain.oauth.repository.OAuthAccountRepository;
import com.kanta.auth.grpc.AuthInternalServiceGrpc;
import com.kanta.auth.grpc.ListOAuthAccountsRequest;
import com.kanta.auth.grpc.ListOAuthAccountsResponse;
import com.kanta.auth.grpc.OAuthAccountView;
import io.grpc.stub.StreamObserver;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class AuthGrpcServer extends AuthInternalServiceGrpc.AuthInternalServiceImplBase {
    private final OAuthAccountRepository oAuthAccountRepository;

    public AuthGrpcServer(OAuthAccountRepository oAuthAccountRepository) {
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    @Override
    public void listOAuthAccounts(ListOAuthAccountsRequest request, StreamObserver<ListOAuthAccountsResponse> responseObserver) {
        var accounts = oAuthAccountRepository.findByUserId(UUID.fromString(request.getUserId()));

        var builder = ListOAuthAccountsResponse.newBuilder();
        for (var account : accounts) {
            builder.addAccounts(
                OAuthAccountView.newBuilder()
                    .setProvider(account.getProvider())
                    .setLinkedAt(DateTimeFormatter.ISO_INSTANT.format(account.getLinkedAt()))
                    .build()
            );
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
