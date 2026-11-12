package com.lms.identity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "lms.auth")
public class AuthProperties {
    private String jwtSecret;
    private Duration sessionTtl = Duration.ofHours(2);
}
