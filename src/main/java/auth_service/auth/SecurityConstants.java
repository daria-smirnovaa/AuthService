package auth_service.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class SecurityConstants {
    private String authHeader;
    private String bearerPrefix;
    private String accessSecret;
    private Integer accessLifetime;
    private String refreshSecret;
    private Integer refreshLifetime;
}

