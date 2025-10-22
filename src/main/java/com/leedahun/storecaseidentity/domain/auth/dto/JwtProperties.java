package com.leedahun.storecaseidentity.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(value = "jwt")
public class JwtProperties {
    private String secret;
    private long expirationTime;
    private long refreshExpirationTime;
}
