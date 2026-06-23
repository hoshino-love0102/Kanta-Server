package com.kanta.user.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanta.auth.grpc.OAuthAccountView;
import com.kanta.user.common.NotFoundException;
import com.kanta.user.domain.user.entity.User;
import com.kanta.user.domain.user.repository.UserRepository;
import com.kanta.user.infrastructure.grpc.AuthGrpcClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventWriterDelegate outboxEventWriterDelegate;

    @Mock
    private AuthGrpcClient authGrpcClient;

    @InjectMocks
    private UserService userService;

    @Test
    void getMe_사용자가_존재하면_oauthAccounts를_포함한_프로필을_반환한다() {
        var user = new User("user@kanta.com", "카리나");
        var userId = setId(user);

        var oauthView = OAuthAccountView.newBuilder()
            .setProvider("GOOGLE")
            .setLinkedAt("2024-01-01T00:00:00Z")
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authGrpcClient.listOAuthAccounts(userId.toString())).thenReturn(List.of(oauthView));

        var response = userService.getMe(userId.toString());

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("user@kanta.com");
        assertThat(response.displayName()).isEqualTo("카리나");
        assertThat(response.oauthAccounts()).hasSize(1);
        assertThat(response.oauthAccounts().get(0).provider()).isEqualTo("GOOGLE");
        assertThat(response.oauthAccounts().get(0).linkedAt()).isEqualTo("2024-01-01T00:00:00Z");
    }

    @Test
    void getMe_사용자가_존재하지_않으면_NotFoundException을_던진다() {
        var userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(userId.toString()))
            .isInstanceOf(NotFoundException.class)
            .satisfies(ex -> assertThat(((NotFoundException) ex).getErrorCode()).isEqualTo("USER_NOT_FOUND"));
    }

    @Test
    void patchMe_displayName이_변경되면_rename과_outbox_이벤트가_호출된다() {
        var user = new User("user@kanta.com", "카리나");
        var userId = setId(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authGrpcClient.listOAuthAccounts(any())).thenReturn(List.of());

        userService.patchMe(userId.toString(), "윈터");

        assertThat(user.getDisplayName()).isEqualTo("윈터");
        verify(outboxEventWriterDelegate, times(1)).appendUserUpdated(user);
    }

    @Test
    void patchMe_displayName이_기존값과_동일하면_rename과_outbox_이벤트가_호출되지_않는다() {
        var user = new User("user@kanta.com", "카리나");
        var userId = setId(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authGrpcClient.listOAuthAccounts(any())).thenReturn(List.of());

        userService.patchMe(userId.toString(), "카리나");

        assertThat(user.getDisplayName()).isEqualTo("카리나");
        verify(outboxEventWriterDelegate, never()).appendUserUpdated(any());
    }

    @Test
    void patchMe_displayName이_null이면_rename과_outbox_이벤트가_호출되지_않는다() {
        var user = new User("user@kanta.com", "카리나");
        var userId = setId(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authGrpcClient.listOAuthAccounts(any())).thenReturn(List.of());

        userService.patchMe(userId.toString(), null);

        assertThat(user.getDisplayName()).isEqualTo("카리나");
        verify(outboxEventWriterDelegate, never()).appendUserUpdated(any());
    }

    @Test
    void upsert_새로운_이메일이면_사용자를_생성하고_userCreated_이벤트가_호출된다() {
        when(userRepository.findByEmail("new@kanta.com")).thenReturn(Optional.empty());

        var result = userService.upsert("new@kanta.com", "닝닝");

        assertThat(result.email()).isEqualTo("new@kanta.com");
        assertThat(result.displayName()).isEqualTo("닝닝");
        verify(userRepository, times(1)).save(any(User.class));
        verify(outboxEventWriterDelegate, times(1)).appendUserCreated(any(User.class));
        verify(outboxEventWriterDelegate, never()).appendUserUpdated(any());
    }

    @Test
    void upsert_기존_이메일이고_displayName이_다르면_rename과_userUpdated_이벤트가_호출된다() {
        var user = new User("existing@kanta.com", "카리나");
        setId(user);

        when(userRepository.findByEmail("existing@kanta.com")).thenReturn(Optional.of(user));

        var result = userService.upsert("existing@kanta.com", "윈터");

        assertThat(user.getDisplayName()).isEqualTo("윈터");
        assertThat(result.displayName()).isEqualTo("윈터");
        verify(outboxEventWriterDelegate, times(1)).appendUserUpdated(user);
        verify(userRepository, never()).save(any());
        verify(outboxEventWriterDelegate, never()).appendUserCreated(any());
    }

    @Test
    void upsert_기존_이메일이고_displayName이_동일하면_rename과_이벤트가_호출되지_않는다() {
        var user = new User("existing@kanta.com", "카리나");
        setId(user);

        when(userRepository.findByEmail("existing@kanta.com")).thenReturn(Optional.of(user));

        userService.upsert("existing@kanta.com", "카리나");

        assertThat(user.getDisplayName()).isEqualTo("카리나");
        verify(outboxEventWriterDelegate, never()).appendUserUpdated(any());
        verify(outboxEventWriterDelegate, never()).appendUserCreated(any());
    }

    @Test
    void upsert_기존_이메일이고_displayName이_blank이면_rename과_이벤트가_호출되지_않는다() {
        var user = new User("existing@kanta.com", "카리나");
        setId(user);

        when(userRepository.findByEmail("existing@kanta.com")).thenReturn(Optional.of(user));

        userService.upsert("existing@kanta.com", "   ");

        assertThat(user.getDisplayName()).isEqualTo("카리나");
        verify(outboxEventWriterDelegate, never()).appendUserUpdated(any());
        verify(outboxEventWriterDelegate, never()).appendUserCreated(any());
    }

    @Test
    void upsert_기존_이메일이고_displayName이_null이면_rename과_이벤트가_호출되지_않는다() {
        var user = new User("existing@kanta.com", "카리나");
        setId(user);

        when(userRepository.findByEmail("existing@kanta.com")).thenReturn(Optional.of(user));

        userService.upsert("existing@kanta.com", null);

        assertThat(user.getDisplayName()).isEqualTo("카리나");
        verify(outboxEventWriterDelegate, never()).appendUserUpdated(any());
        verify(outboxEventWriterDelegate, never()).appendUserCreated(any());
    }

    private UUID setId(User user) {
        var id = UUID.randomUUID();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return id;
    }
}
