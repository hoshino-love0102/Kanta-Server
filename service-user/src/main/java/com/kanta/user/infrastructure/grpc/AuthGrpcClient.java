package com.kanta.user.infrastructure.grpc;

import com.kanta.auth.grpc.AuthInternalServiceGrpc;
import com.kanta.auth.grpc.ListOAuthAccountsRequest;
import com.kanta.auth.grpc.OAuthAccountView;
import java.util.List;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class AuthGrpcClient {
    @GrpcClient("service-auth")
    private AuthInternalServiceGrpc.AuthInternalServiceBlockingStub stub;

    public List<OAuthAccountView> listOAuthAccounts(String userId) {
        var response = stub.listOAuthAccounts(
            ListOAuthAccountsRequest.newBuilder().setUserId(userId).build()
        );
        return response.getAccountsList();
    }
}
