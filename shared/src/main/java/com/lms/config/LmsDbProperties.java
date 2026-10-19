package com.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "lms.db")
public class LmsDbProperties {
    private String host = "localhost";
    private int port = 5432;
    private String username = "lms";
    private String password = "lms";
}
