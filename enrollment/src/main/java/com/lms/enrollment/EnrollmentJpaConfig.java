package com.lms.enrollment;

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
        basePackages = "com.lms.enrollment.repo",
        entityManagerFactoryRef = "enrollmentEmf",
        transactionManagerRef = "enrollmentTx"
)
public class EnrollmentJpaConfig {
    @Bean
    public DataSource enrollmentDataSource(LmsDbProperties db) {
        return JpaSupport.dataSource("lms_enrollment", db);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean enrollmentEmf(
            @Qualifier("enrollmentDataSource") DataSource ds) {
        return JpaSupport.entityManagerFactory(ds, "enrollment", "com.lms.enrollment.entity");
    }

    @Bean
    public JpaTransactionManager enrollmentTx(
            @Qualifier("enrollmentEmf") LocalContainerEntityManagerFactoryBean emf) {
        return JpaSupport.transactionManager(emf);
    }
}
