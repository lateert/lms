package com.lms.grpc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "lms.grpc")
public class GrpcProperties {
    private String identity = "localhost:9101";
    private String catalog = "localhost:9102";
    private String enrollment = "localhost:9103";
}
