package com.miguelbf.exchangerateapi.repository;

import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryIT {

    @Container
    public static PostgreSQLContainer container = new PostgreSQLContainer("postgres:latest")
        .withUsername("test")
        .withPassword("test")
        .withDatabaseName("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void givenNoUsers_whenFindByEmail_thenReturnNull() {
        User user = userRepository.findByEmail("notfound@example.com");

        assertNull(user);
    }

    @Test
    void givenExistingUser_whenFindByEmail_thenReturnUser() {
        User savedUser = new User("found@example.com", "password", UserRole.FREE_TIER);
        savedUser = userRepository.save(savedUser);

        User user = userRepository.findByEmail(savedUser.getUsername());

        assertNotNull(user);
        assertEquals(savedUser, user);
    }

}
