package net.ent.etnc.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Externalized refresh-token cookie, expiration and audit settings.
 */
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
    private Duration expiration = Duration.ofHours(2);

    /**
     * Returns refresh token expiration.
     */
    public Duration getExpiration() {
        return expiration;
    }

    /**
     * Sets refresh token expiration.
     */
    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    /**
     * Returns cookie settings.
     */
    public Cookie getCookie() {
        return cookie;
    }

    /**
     * Returns audit settings.
     */
    public Audit getAudit() {
        return audit;
    }

    /**
     * Refresh cookie attributes.
     */
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

        /**
         * Returns cookie name.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets cookie name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns Secure flag.
         */
        public boolean isSecure() {
            return secure;
        }

        /**
         * Sets Secure flag.
         */
        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        /**
         * Returns SameSite value.
         */
        public String getSameSite() {
            return sameSite;
        }

        /**
         * Sets SameSite value.
         */
        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }

        /**
         * Returns cookie path.
         */
        public String getPath() {
            return path;
        }

        /**
         * Sets cookie path.
         */
        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * Audit-related settings for refresh tokens.
     */
    public static class Audit {
        /**
         * Whether X-Forwarded-For is trusted.
         */
        private boolean trustForwardedFor = false;

        /**
         * Returns whether X-Forwarded-For is trusted.
         */
        public boolean isTrustForwardedFor() {
            return trustForwardedFor;
        }

        /**
         * Sets whether X-Forwarded-For is trusted.
         */
        public void setTrustForwardedFor(boolean trustForwardedFor) {
            this.trustForwardedFor = trustForwardedFor;
        }
    }
}
