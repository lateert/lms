package com.lms.assessment;

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
        basePackages = "com.lms.assessment.repo",
        entityManagerFactoryRef = "assessmentEmf",
        transactionManagerRef = "assessmentTx"
)
public class AssessmentJpaConfig {
    @Bean
    public DataSource assessmentDataSource(LmsDbProperties db) {
        return JpaSupport.dataSource("lms_assessment", db);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean assessmentEmf(
            @Qualifier("assessmentDataSource") DataSource ds) {
        return JpaSupport.entityManagerFactory(ds, "assessment", "com.lms.assessment.entity");
    }

    @Bean
    public JpaTransactionManager assessmentTx(
            @Qualifier("assessmentEmf") LocalContainerEntityManagerFactoryBean emf) {
        return JpaSupport.transactionManager(emf);
    }
}
