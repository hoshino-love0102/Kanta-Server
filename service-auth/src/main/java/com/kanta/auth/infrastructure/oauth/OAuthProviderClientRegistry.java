package com.kanta.auth.infrastructure.oauth;

import com.kanta.auth.common.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OAuthProviderClientRegistry {
    private final Map<String, OAuthProviderClient> clientsByProvider;

    public OAuthProviderClientRegistry(List<OAuthProviderClient> clients) {
        this.clientsByProvider = clients.stream()
            .collect(Collectors.toMap(OAuthProviderClient::provider, Function.identity()));
    }

    public OAuthProviderClient resolve(String provider) {
        var client = clientsByProvider.get(provider.toUpperCase());
        if (client == null) {
            throw new NotFoundException("지원하지 않는 OAuth provider입니다.", "UNSUPPORTED_PROVIDER");
        }
        return client;
    }
}
