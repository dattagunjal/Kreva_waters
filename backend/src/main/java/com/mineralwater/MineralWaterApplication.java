package com.mineralwater;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class MineralWaterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MineralWaterApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds connection timeout
        factory.setReadTimeout(5000);    // 5 seconds read timeout
        return new RestTemplate(factory);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner databaseInitializer(javax.sql.DataSource dataSource) {
        return args -> {
            try (java.sql.Connection conn = dataSource.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                // Ensure email and mobile_number columns are nullable in the database
                try {
                    stmt.execute("ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL");
                } catch (Exception e) {
                    System.err.println("Could not alter email column: " + e.getMessage());
                }
                try {
                    stmt.execute("ALTER TABLE users MODIFY COLUMN mobile_number VARCHAR(255) NULL");
                } catch (Exception e) {
                    System.err.println("Could not alter mobile_number column: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Database initialization error: " + e.getMessage());
            }
        };
    }
}
