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
    public org.springframework.boot.CommandLineRunner databaseInitializer(
            javax.sql.DataSource dataSource,
            com.mineralwater.repository.CategoryRepository categoryRepository,
            com.mineralwater.repository.ProductRepository productRepository,
            com.mineralwater.repository.UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                try (java.sql.Connection conn = dataSource.getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    String dbName = conn.getMetaData().getDatabaseProductName();
                    boolean isPg = dbName != null && dbName.toLowerCase().contains("postgresql");

                    // Ensure email and mobile_number columns are nullable in the database
                    try {
                        if (isPg) {
                            stmt.execute("ALTER TABLE users ALTER COLUMN email DROP NOT NULL");
                        } else {
                            stmt.execute("ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL");
                        }
                    } catch (Exception e) {
                        System.err.println("Could not alter email column: " + e.getMessage());
                    }
                    try {
                        if (isPg) {
                            stmt.execute("ALTER TABLE users ALTER COLUMN mobile_number DROP NOT NULL");
                        } else {
                            stmt.execute("ALTER TABLE users MODIFY COLUMN mobile_number VARCHAR(255) NULL");
                        }
                    } catch (Exception e) {
                        System.err.println("Could not alter mobile_number column: " + e.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Database initialization error: " + e.getMessage());
                }

                // Seed default category if none exists
                if (categoryRepository.count() == 0) {
                    com.mineralwater.model.Category cat = com.mineralwater.model.Category.builder()
                            .name("Mineral Water")
                            .build();
                    cat = categoryRepository.save(cat);

                    // Seed default products
                    if (productRepository.count() == 0) {
                        productRepository.save(com.mineralwater.model.Product.builder()
                                .name("Kreva 200ml")
                                .price(5.0)
                                .stock(1000)
                                .description("Refreshing premium mineral water in a 200ml bottle.")
                                .category(cat)
                                .imageUrl("assets/images/bottle_200ml.png")
                                .build());

                        productRepository.save(com.mineralwater.model.Product.builder()
                                .name("Kreva 500ml")
                                .price(10.0)
                                .stock(1000)
                                .description("Refreshing premium mineral water in a 500ml bottle.")
                                .category(cat)
                                .imageUrl("assets/images/bottle_500ml.png")
                                .build());

                        productRepository.save(com.mineralwater.model.Product.builder()
                                .name("Kreva 1 Litre")
                                .price(20.0)
                                .stock(1000)
                                .description("Refreshing premium mineral water in a 1 Litre bottle.")
                                .category(cat)
                                .imageUrl("assets/images/bottle_1l.png")
                                .build());

                        productRepository.save(com.mineralwater.model.Product.builder()
                                .name("Kreva 20 Litre Can")
                                .price(80.0)
                                .stock(1000)
                                .description("Refreshing premium mineral water in a 20 Litre reusable can.")
                                .category(cat)
                                .imageUrl("assets/images/can_20l.png")
                                .build());
                    }
                }

                // Seed default admin user if none exists
                if (!userRepository.existsByEmail("admin@Kreva.in")) {
                    com.mineralwater.model.User admin = com.mineralwater.model.User.builder()
                            .name("Admin")
                            .email("admin@Kreva.in")
                            .password(passwordEncoder.encode("admin123"))
                            .role(com.mineralwater.model.User.Role.ADMIN)
                            .build();
                    userRepository.save(admin);
                }
            } catch (Exception e) {
                System.err.println("Database seeding/init failed but application will continue boot: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
