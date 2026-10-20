package com.lms.identity;

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
        basePackages = "com.lms.identity.repo",
        entityManagerFactoryRef = "identityEmf",
        transactionManagerRef = "identityTx"
)
public class IdentityJpaConfig {
    @Bean
    public DataSource identityDataSource(LmsDbProperties db) {
        return JpaSupport.dataSource("lms_identity", db);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean identityEmf(
            @Qualifier("identityDataSource") DataSource ds) {
        return JpaSupport.entityManagerFactory(ds, "identity", "com.lms.identity.entity");
    }

    @Bean
    public JpaTransactionManager identityTx(
            @Qualifier("identityEmf") LocalContainerEntityManagerFactoryBean emf) {
        return JpaSupport.transactionManager(emf);
    }
}
