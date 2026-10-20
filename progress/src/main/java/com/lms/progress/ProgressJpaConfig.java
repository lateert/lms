package com.lms.progress;

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
        basePackages = "com.lms.progress.repo",
        entityManagerFactoryRef = "progressEmf",
        transactionManagerRef = "progressTx"
)
public class ProgressJpaConfig {
    @Bean
    public DataSource progressDataSource(LmsDbProperties db) {
        return JpaSupport.dataSource("lms_progress", db);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean progressEmf(
            @Qualifier("progressDataSource") DataSource ds) {
        return JpaSupport.entityManagerFactory(ds, "progress", "com.lms.progress.entity");
    }

    @Bean
    public JpaTransactionManager progressTx(
            @Qualifier("progressEmf") LocalContainerEntityManagerFactoryBean emf) {
        return JpaSupport.transactionManager(emf);
    }
}
