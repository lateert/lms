package com.lms;

import com.lms.config.LmsDbProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties(LmsDbProperties.class)
public class LmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }
}
