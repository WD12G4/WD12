package net.ent.etnc.security.jwt;

import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtils Unit Tests")
class JwtUtilsTest {

    private static final String TEST_SECRET = "superSecretKeyForTestingPurposesMustBeAtLeast256BitsLong123456";
    private JwtUtils jwtUtils;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000L); // 1 hour

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setRole(ERole.USER);
    }

    @Test
    @DisplayName("Doit générer un token valide à partir d'un utilisateur et extraire son username")
    void generateJwtTokenForUser_Success() {
        String token = jwtUtils.generateJwtTokenForUser(sampleUser);

        assertThat(token).isNotBlank();
        Optional<String> username = jwtUtils.getUsernameIfValid(token);
        assertThat(username).isPresent().contains("testuser");
    }

    @Test
    @DisplayName("Doit générer un token valide à partir d'un objet Authentication")
    void generateJwtToken_FromAuthentication_Success() {
        Authentication auth = new UsernamePasswordAuthenticationToken(sampleUser, null, sampleUser.getAuthorities());

        String token = jwtUtils.generateJwtToken(auth);

        assertThat(token).isNotBlank();
        Optional<String> username = jwtUtils.getUsernameIfValid(token);
        assertThat(username).isPresent().contains("testuser");
    }

    @Test
    @DisplayName("getUsernameIfValid doit retourner empty si le token est null ou vide")
    void getUsernameIfValid_WhenNullOrBlank_ReturnsEmpty() {
        assertThat(jwtUtils.getUsernameIfValid(null)).isEmpty();
        assertThat(jwtUtils.getUsernameIfValid("")).isEmpty();
        assertThat(jwtUtils.getUsernameIfValid("   ")).isEmpty();
    }

    @Test
    @DisplayName("getUsernameIfValid doit retourner empty si le token est invalide ou corrompu")
    void getUsernameIfValid_WhenMalformedToken_ReturnsEmpty() {
        assertThat(jwtUtils.getUsernameIfValid("invalid.jwt.token")).isEmpty();
    }

    @Test
    @DisplayName("getUsernameIfValid doit retourner empty si le token est signé avec une clé différente")
    void getUsernameIfValid_WhenSignedWithDifferentKey_ReturnsEmpty() {
        JwtUtils otherJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(otherJwtUtils, "secret", "anotherSecretKeyForTestingPurposesMustBeAtLeast256BitsLong9999");
        ReflectionTestUtils.setField(otherJwtUtils, "jwtExpirationMs", 3600000L);

        String tokenFromOther = otherJwtUtils.generateJwtTokenForUser(sampleUser);

        assertThat(jwtUtils.getUsernameIfValid(tokenFromOther)).isEmpty();
    }

    @Test
    @DisplayName("getUsernameIfValid doit retourner empty si le token est expiré")
    void getUsernameIfValid_WhenTokenExpired_ReturnsEmpty() {
        JwtUtils expiredJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(expiredJwtUtils, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(expiredJwtUtils, "jwtExpirationMs", -1000L); // Expired immediately

        String expiredToken = expiredJwtUtils.generateJwtTokenForUser(sampleUser);

        assertThat(jwtUtils.getUsernameIfValid(expiredToken)).isEmpty();
    }
}
