package com.kanta.auth.application.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanta.auth.common.DomainException;
import com.kanta.auth.domain.token.entity.RefreshToken;
import com.kanta.auth.domain.token.repository.RefreshTokenRepository;
import com.kanta.auth.infrastructure.jwt.RefreshTokenGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenIssuerTest {

    private static final long TTL_DAYS = 14;

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenGenerator refreshTokenGenerator;
    private RefreshTokenIssuer refreshTokenIssuer;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        refreshTokenGenerator = mock(RefreshTokenGenerator.class);
        refreshTokenIssuer = new RefreshTokenIssuer(refreshTokenRepository, refreshTokenGenerator, TTL_DAYS);
    }

    @Test
    void issue는_새로운_refresh_token을_생성하고_저장한다() {
        var userId = UUID.randomUUID();
        when(refreshTokenGenerator.generate()).thenReturn("raw-token");
        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");

        var result = refreshTokenIssuer.issue(userId);

        assertThat(result).isEqualTo("raw-token");
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void issue는_저장되는_토큰에_올바른_userId와_tokenHash를_담는다() {
        var userId = UUID.randomUUID();
        when(refreshTokenGenerator.generate()).thenReturn("raw-token");
        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");

        var captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        refreshTokenIssuer.issue(userId);

        verify(refreshTokenRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTokenHash()).isEqualTo("hashed-token");
    }

    @Test
    void issue는_TTL_일수에_맞춰_만료시각을_설정한다() {
        var userId = UUID.randomUUID();
        when(refreshTokenGenerator.generate()).thenReturn("raw-token");
        when(refreshTokenGenerator.hash("raw-token")).thenReturn("hashed-token");

        var before = Instant.now();
        var captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        refreshTokenIssuer.issue(userId);
        var after = Instant.now();

        verify(refreshTokenRepository).save(captor.capture());
        var expiresAt = captor.getValue().getExpiresAt();

        assertThat(expiresAt).isAfter(before.plusSeconds(TTL_DAYS * 24 * 60 * 60 - 5));
        assertThat(expiresAt).isBefore(after.plusSeconds(TTL_DAYS * 24 * 60 * 60 + 5));
    }

    @Test
    void reissue는_새_토큰을_저장하고_이전_토큰을_회전시킨다() {
        var userId = UUID.randomUUID();
        var previous = new RefreshToken(userId, "old-hash", Instant.now().plusSeconds(60));
        when(refreshTokenGenerator.generate()).thenReturn("new-raw-token");
        when(refreshTokenGenerator.hash("new-raw-token")).thenReturn("new-hashed-token");
        when(refreshTokenRepository.revokeIfActive(any(), any(), any())).thenReturn(1);

        var result = refreshTokenIssuer.reissue(userId, previous);

        assertThat(result).isEqualTo("new-raw-token");
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        verify(refreshTokenRepository, times(1)).revokeIfActive(any(), any(), any());
    }

    @Test
    void reissue는_revokeIfActive가_0건이면_INVALID_REFRESH_TOKEN_예외를_던진다() {
        var userId = UUID.randomUUID();
        var previous = new RefreshToken(userId, "old-hash", Instant.now().plusSeconds(60));
        when(refreshTokenGenerator.generate()).thenReturn("new-raw-token");
        when(refreshTokenGenerator.hash("new-raw-token")).thenReturn("new-hashed-token");
        when(refreshTokenRepository.revokeIfActive(any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> refreshTokenIssuer.reissue(userId, previous))
            .isInstanceOf(DomainException.class)
            .satisfies(exception -> {
                var domainException = (DomainException) exception;
                assertThat(domainException.getErrorCode()).isEqualTo("INVALID_REFRESH_TOKEN");
                assertThat(domainException.getStatus().value()).isEqualTo(401);
            });
    }

    @Test
    void reissue는_이전_토큰의_id를_사용해서_revokeIfActive를_호출한다() {
        var userId = UUID.randomUUID();
        var previous = mock(RefreshToken.class);
        var previousId = UUID.randomUUID();
        when(previous.getId()).thenReturn(previousId);
        when(refreshTokenGenerator.generate()).thenReturn("new-raw-token");
        when(refreshTokenGenerator.hash("new-raw-token")).thenReturn("new-hashed-token");
        when(refreshTokenRepository.revokeIfActive(any(), any(), any())).thenReturn(1);

        refreshTokenIssuer.reissue(userId, previous);

        verify(refreshTokenRepository).revokeIfActive(org.mockito.ArgumentMatchers.eq(previousId), any(), any());
    }
}
