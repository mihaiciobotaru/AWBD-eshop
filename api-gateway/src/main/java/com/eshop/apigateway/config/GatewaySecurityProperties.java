package com.eshop.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Base64; // Import Base64

/**
 * Configuration properties for Gateway Security settings,
 * including JWT secret keys and internal gateway secrets.
 * Secret keys are expected to be Base64 encoded in properties
 * and are decoded upon loading.
 */
@Component
@ConfigurationProperties(prefix = "spring.cloud.gateway")
public class GatewaySecurityProperties {

    private Jwt jwt = new Jwt();
    private InternalGateway internalGateway = new InternalGateway();

    public static class Jwt {
        private String secretKey;

        // Getter that decodes the secret key from Base64
        public byte[] getDecodedSecretKey() {
            if (secretKey == null || secretKey.isEmpty()) {
                throw new IllegalArgumentException("JWT secret key cannot be null or empty.");
            }
            // Decode from Base64. URL-safe decoding might be better depending on origin.
            return Base64.getDecoder().decode(secretKey);
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getSecretKey() {
            return secretKey;
        }
    }

    public static class InternalGateway {
        private String secretKey;

        // Getter that decodes the secret key from Base64
        public byte[] getDecodedSecretKey() {
            if (secretKey == null || secretKey.isEmpty()) {
                throw new IllegalArgumentException("Internal Gateway secret key cannot be null or empty.");
            }
            return Base64.getDecoder().decode(secretKey);
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getSecretKey() {
            return secretKey;
        }
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public InternalGateway getInternalGateway() {
        return internalGateway;
    }

    public void setInternalGateway(InternalGateway internalGateway) {
        this.internalGateway = internalGateway;
    }
}
