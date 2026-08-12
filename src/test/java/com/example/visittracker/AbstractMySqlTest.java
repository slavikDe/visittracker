package com.example.visittracker;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base for tests that need a real database.
 * <p>
 * The read path leans on MySQL 8 specifics — {@code ROW_NUMBER()} windows, descending indexes,
 * {@code LIKE CONCAT(?, '%')} — so an in-memory substitute would not prove much.
 * <p>
 * The container is a bean rather than a JUnit-managed {@code @Container} field: Spring Boot then
 * owns its lifecycle and derives the datasource URL from it, and the cached application context
 * keeps one MySQL instance shared across every test class that extends this.
 */
@SpringBootTest
@Import(AbstractMySqlTest.MySqlContainerConfiguration.class)
public abstract class AbstractMySqlTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class MySqlContainerConfiguration {

        @Bean
        @ServiceConnection
        MySQLContainer mysqlContainer() {
            return new MySQLContainer("mysql:8.4");
        }
    }
}
