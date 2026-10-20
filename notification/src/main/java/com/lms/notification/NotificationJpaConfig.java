package com.lms.notification;

import com.lms.config.JpaSupport;
import com.lms.config.LmsDbProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.lms.notification.repo",
        entityManagerFactoryRef = "notificationEmf",
        transactionManagerRef = "notificationTx"
)
public class NotificationJpaConfig {
    @Bean
    public DataSource notificationDataSource(LmsDbProperties db) {
        return JpaSupport.dataSource("lms_notification", db);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean notificationEmf(
            @Qualifier("notificationDataSource") DataSource ds) {
        return JpaSupport.entityManagerFactory(ds, "notification", "com.lms.notification.entity");
    }

    @Bean
    public JpaTransactionManager notificationTx(
            @Qualifier("notificationEmf") LocalContainerEntityManagerFactoryBean emf) {
        return JpaSupport.transactionManager(emf);
    }
}
