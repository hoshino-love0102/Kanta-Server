package com.kanta.user.application.user;

import com.kanta.user.common.NotFoundException;
import com.kanta.user.domain.user.entity.User;
import com.kanta.user.domain.user.repository.UserRepository;
import com.kanta.user.infrastructure.grpc.AuthGrpcClient;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final OutboxEventWriterDelegate outboxEventWriterDelegate;
    private final AuthGrpcClient authGrpcClient;

    public UserService(
        UserRepository userRepository,
        OutboxEventWriterDelegate outboxEventWriterDelegate,
        AuthGrpcClient authGrpcClient
    ) {
        this.userRepository = userRepository;
        this.outboxEventWriterDelegate = outboxEventWriterDelegate;
        this.authGrpcClient = authGrpcClient;
    }

    public UserProfileResponse getMe(String userId) {
        var user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"));
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse patchMe(String userId, String displayName) {
        var user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"));

        if (displayName != null && !displayName.equals(user.getDisplayName())) {
            user.rename(displayName);
            outboxEventWriterDelegate.appendUserUpdated(user);
        }

        return toProfileResponse(user);
    }

    @Transactional
    public UpsertUserResult upsert(String email, String displayName) {
        var existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            var user = existing.get();
            if (displayName != null && !displayName.isBlank() && !displayName.equals(user.getDisplayName())) {
                user.rename(displayName);
                outboxEventWriterDelegate.appendUserUpdated(user);
            }
            return new UpsertUserResult(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
        }

        var user = new User(email, displayName);
        userRepository.save(user);
        outboxEventWriterDelegate.appendUserCreated(user);
        return new UpsertUserResult(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }

    private UserProfileResponse toProfileResponse(User user) {
        List<OAuthAccountResponse> oauthAccounts = authGrpcClient.listOAuthAccounts(user.getId().toString())
            .stream()
            .map(view -> new OAuthAccountResponse(view.getProvider(), view.getLinkedAt()))
            .toList();

        return new UserProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), oauthAccounts);
    }
}
