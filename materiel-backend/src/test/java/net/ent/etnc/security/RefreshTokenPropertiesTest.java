package net.ent.etnc.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshTokenProperties Unit Tests")
class RefreshTokenPropertiesTest {

    @Test
    @DisplayName("Vérifie les valeurs par défaut et les mutateurs")
    void testPropertiesGettersAndSetters() {
        RefreshTokenProperties properties = new RefreshTokenProperties();

        assertThat(properties.getExpiration()).isEqualTo(Duration.ofHours(2));
        properties.setExpiration(Duration.ofDays(7));
        assertThat(properties.getExpiration()).isEqualTo(Duration.ofDays(7));

        RefreshTokenProperties.Cookie cookie = properties.getCookie();
        assertThat(cookie.getName()).isEqualTo("object_rent_refresh");
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");

        cookie.setName("custom_cookie");
        cookie.setSecure(true);
        cookie.setSameSite("Lax");
        cookie.setPath("/api/v2/auth");

        assertThat(cookie.getName()).isEqualTo("custom_cookie");
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/api/v2/auth");

        RefreshTokenProperties.Audit audit = properties.getAudit();
        assertThat(audit.isTrustForwardedFor()).isFalse();
        audit.setTrustForwardedFor(true);
        assertThat(audit.isTrustForwardedFor()).isTrue();
    }
}
