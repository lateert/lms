package com.lms.catalog;

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
        basePackages = "com.lms.catalog.repo",
        entityManagerFactoryRef = "catalogEmf",
        transactionManagerRef = "catalogTx"
)
public class CatalogJpaConfig {
    @Bean
    public DataSource catalogDataSource(LmsDbProperties db) {
        return JpaSupport.dataSource("lms_catalog", db);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean catalogEmf(
            @Qualifier("catalogDataSource") DataSource ds) {
        return JpaSupport.entityManagerFactory(ds, "catalog", "com.lms.catalog.entity");
    }

    @Bean
    public JpaTransactionManager catalogTx(
            @Qualifier("catalogEmf") LocalContainerEntityManagerFactoryBean emf) {
        return JpaSupport.transactionManager(emf);
    }
}
