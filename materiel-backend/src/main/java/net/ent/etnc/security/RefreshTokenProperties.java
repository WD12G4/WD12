package net.ent.etnc.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Externalized refresh-token cookie, expiration and audit settings.
 */
@Getter
@ConfigurationProperties(prefix = "app.security.refresh-token")
public class RefreshTokenProperties {

    /**
     * Cookie attributes reused for creation and deletion.
     */
    private final Cookie cookie = new Cookie();

    /**
     * Audit metadata settings.
     */
    private final Audit audit = new Audit();

    /**
     * Sliding expiration duration for a newly created refresh token.
     */
    @Setter
    private Duration expiration = Duration.ofHours(2);

    /**
     * Refresh cookie attributes.
     */
    @Setter
    @Getter
    public static class Cookie {

        /**
         * Cookie name.
         */
        private String name = "object_rent_refresh";

        /**
         * Whether Secure is enabled.
         */
        private boolean secure = false;

        /**
         * SameSite value.
         */
        private String sameSite = "Strict";

        /**
         * Cookie path shared by refresh and logout.
         */
        private String path = "/api/v1/auth";

    }

    /**
     * Audit-related settings for refresh tokens.
     */
    @Setter
    @Getter
    public static class Audit {

        /**
         * Whether X-Forwarded-For is trusted.
         */
        private boolean trustForwardedFor = false;

    }
}
