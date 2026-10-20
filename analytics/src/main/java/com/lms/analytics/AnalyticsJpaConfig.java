package com.lms.analytics;

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
        basePackages = "com.lms.analytics.repo",
        entityManagerFactoryRef = "analyticsEmf",
        transactionManagerRef = "analyticsTx"
)
public class AnalyticsJpaConfig {
    @Bean
    public DataSource analyticsDataSource(LmsDbProperties db) {
        return JpaSupport.dataSource("lms_analytics", db);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean analyticsEmf(
            @Qualifier("analyticsDataSource") DataSource ds) {
        return JpaSupport.entityManagerFactory(ds, "analytics", "com.lms.analytics.entity");
    }

    @Bean
    public JpaTransactionManager analyticsTx(
            @Qualifier("analyticsEmf") LocalContainerEntityManagerFactoryBean emf) {
        return JpaSupport.transactionManager(emf);
    }
}
