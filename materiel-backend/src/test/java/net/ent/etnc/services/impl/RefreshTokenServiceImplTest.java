package net.ent.etnc.services.impl;

import net.ent.etnc.models.RefreshToken;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.RefreshTokenRepository;
import net.ent.etnc.security.RefreshTokenProperties;
import net.ent.etnc.security.exceptions.ExpiredRefreshTokenException;
import net.ent.etnc.security.exceptions.InvalidRefreshTokenException;
import net.ent.etnc.security.exceptions.ReusedRefreshTokenException;
import net.ent.etnc.services.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenServiceImpl Unit Tests")
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenProperties properties;
    private Clock fixedClock;
    private RefreshTokenServiceImpl refreshTokenService;

    private User sampleUser;
    private final Instant fixedInstant = Instant.parse("2026-10-22T10:00:00Z");

    @BeforeEach
    void setUp() {
        properties = new RefreshTokenProperties();
        properties.setExpiration(Duration.ofHours(2));
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        refreshTokenService = new RefreshTokenServiceImpl(
                repository,
                properties,
                new SecureRandom(),
                fixedClock
        );

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setRole(ERole.USER);
    }

    @Test
    @DisplayName("Doit créer un token et sauvegarder son hash en base")
    void createToken_Success() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String rawToken = refreshTokenService.createToken(sampleUser, "127.0.0.1", "Mozilla/5.0");

        assertThat(rawToken).isNotBlank();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(sampleUser);
        assertThat(saved.getCreatedAt()).isEqualTo(fixedInstant);
        assertThat(saved.getExpiresAt()).isEqualTo(fixedInstant.plus(Duration.ofHours(2)));
        assertThat(saved.getCreatedByIp()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("Doit tronquer l'IP et le UserAgent s'ils dépassent la taille maximale")
    void createToken_TruncatesLongIpAndUserAgent() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        String longIp = "1".repeat(100);
        String longUa = "U".repeat(600);

        refreshTokenService.createToken(sampleUser, longIp, longUa);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getCreatedByIp()).hasSize(64);
        assertThat(saved.getCreatedByUserAgent()).hasSize(512);
    }

    @Test
    @DisplayName("Doit réussir la rotation d'un token valide et révoquer l'ancien")
    void rotate_Success() {
        RefreshToken existingToken = new RefreshToken(
                "oldHash", "family-1", sampleUser,
                fixedInstant.minus(Duration.ofMinutes(10)),
                fixedInstant.plus(Duration.ofHours(1)),
                "127.0.0.1", "curl"
        );

        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(existingToken));
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshTokenService.RotateResult result = refreshTokenService.rotate("someRawToken", "127.0.0.1", "curl");

        assertThat(result).isNotNull();
        assertThat(result.user()).isEqualTo(sampleUser);
        assertThat(result.newRawToken()).isNotBlank();
        assertThat(existingToken.isRevoked()).isTrue();
        assertThat(existingToken.getReplacedByTokenHash()).isNotNull();
    }

    @Test
    @DisplayName("Doit lever InvalidRefreshTokenException si le token à faire tourner est null ou blanc")
    void rotate_NullOrBlank_ThrowsInvalidToken() {
        assertThatThrownBy(() -> refreshTokenService.rotate(null, "127.0.0.1", "curl"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThatThrownBy(() -> refreshTokenService.rotate("   ", "127.0.0.1", "curl"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Doit lever InvalidRefreshTokenException si le token est inconnu")
    void rotate_ThrowsInvalidToken_WhenNotFound() {
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknownToken", "127.0.0.1", "curl"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Doit détecter la réutilisation d'un token révoqué et révoquer toute la famille")
    void rotate_DetectsReuse_RevokesFamily() {
        RefreshToken revokedToken = new RefreshToken(
                "revokedHash", "family-reuse", sampleUser,
                fixedInstant.minus(Duration.ofHours(1)),
                fixedInstant.plus(Duration.ofHours(1)),
                "127.0.0.1", "curl"
        );
        revokedToken.revoke(fixedInstant.minus(Duration.ofMinutes(30)), "replacementHash");

        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.rotate("reusedToken", "127.0.0.1", "curl"))
                .isInstanceOf(ReusedRefreshTokenException.class);

        verify(repository).revokeActiveByFamilyId("family-reuse", fixedInstant);
    }

    @Test
    @DisplayName("Doit lever ExpiredRefreshTokenException si le token est expiré")
    void rotate_DetectsExpired_ThrowsExpired() {
        RefreshToken expiredToken = new RefreshToken(
                "expiredHash", "family-expired", sampleUser,
                fixedInstant.minus(Duration.ofHours(3)),
                fixedInstant.minus(Duration.ofMinutes(1)),
                "127.0.0.1", "curl"
        );

        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.rotate("expiredToken", "127.0.0.1", "curl"))
                .isInstanceOf(ExpiredRefreshTokenException.class);

        assertThat(expiredToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Doit révoquer le token lors de la déconnexion")
    void revokeToken_Success() {
        RefreshToken activeToken = new RefreshToken(
                "hash", "family-logout", sampleUser,
                fixedInstant.minus(Duration.ofMinutes(10)),
                fixedInstant.plus(Duration.ofHours(1)),
                "127.0.0.1", "curl"
        );

        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(activeToken));

        refreshTokenService.revokeToken("rawTokenToLogout");

        assertThat(activeToken.isRevoked()).isTrue();
        assertThat(activeToken.getRevokedAt()).isEqualTo(fixedInstant);
    }

    @Test
    @DisplayName("Ne doit rien faire si le token à révoquer est null ou blanc")
    void revokeToken_NullOrBlank_DoesNothing() {
        refreshTokenService.revokeToken(null);
        refreshTokenService.revokeToken("   ");

        verifyNoInteractions(repository);
    }
}
