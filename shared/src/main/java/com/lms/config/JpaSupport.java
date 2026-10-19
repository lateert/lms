package com.lms.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public final class JpaSupport {
    private JpaSupport() {
    }

    public static DataSource dataSource(String database, LmsDbProperties db) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName(database);
        ds.setJdbcUrl("jdbc:postgresql://%s:%d/%s".formatted(db.getHost(), db.getPort(), database));
        ds.setUsername(db.getUsername());
        ds.setPassword(db.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        return ds;
    }

    public static LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, String persistenceUnit, String entityPackage) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(entityPackage);
        emf.setPersistenceUnitName(persistenceUnit);
        HibernateJpaVendorAdapter vendor = new HibernateJpaVendorAdapter();
        vendor.setGenerateDdl(true);
        emf.setJpaVendorAdapter(vendor);
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.globally_quoted_identifiers", "true");
        emf.setJpaPropertyMap(props);
        return emf;
    }

    public static JpaTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean emf) {
        JpaTransactionManager tx = new JpaTransactionManager();
        tx.setEntityManagerFactory(emf.getObject());
        return tx;
    }
}
