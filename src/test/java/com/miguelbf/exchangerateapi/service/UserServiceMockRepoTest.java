package com.miguelbf.exchangerateapi.service;


import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.entities.UserRole;
import com.miguelbf.exchangerateapi.repository.UserRepository;
import com.miguelbf.exchangerateapi.service.impl.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceMockRepoTest {

    @InjectMocks
    UserService userService;

    @Mock
    UserRepository userRepository;

    @Test
    void givenNoMatchingUser_whenFindUserByEmail_returnNull() {
        String notFoundEmail = "not-found@example.com";
        when(userRepository.findByEmail(notFoundEmail)).thenReturn(null);

        User user = userService.getUserByEmail(notFoundEmail);

        assertNull(user);
    }

    @Test
    void givenMatchingUser_whenFindUserByEmail_returnUser() {
        User expectedUser = new User("found@example.com", "password", UserRole.FREE_TIER);
        when(userRepository.findByEmail(expectedUser.getUsername())).thenReturn(expectedUser);

        User foundUser = userService.getUserByEmail(expectedUser.getUsername());

        assertNotNull(foundUser);
        assertEquals(expectedUser, foundUser);
    }

    @Test
    void givenValidUser_whenCreateUser_returnSavedUser() {
        User newUser = new User("user@example.com", "password", UserRole.FREE_TIER);
        when(userRepository.save(newUser)).thenReturn(newUser);

        User savedUser = userService.createUser(newUser);

        assertNotNull(savedUser);
        assertEquals(newUser, savedUser);
    }

    @Test
    void givenNoMatchingUser_whenLoadUserByUsername_throwsException() {
        String notFoundEmail = "not-found@example.com";
        when(userRepository.findByEmail(notFoundEmail)).thenReturn(null);
        UserDetailsService userDetailsService = userService.userDetailsService();

        UsernameNotFoundException ex = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(notFoundEmail)
        );

        assertEquals("User by that email not found", ex.getMessage());
    }

    @Test
    void givenMatchingUser_whenLoadUserByUsername_returnUserDetails() {
        User expectedUser = new User("found@example.com", "password", UserRole.FREE_TIER);
        when(userRepository.findByEmail(expectedUser.getUsername())).thenReturn(expectedUser);
        UserDetailsService userDetailsService = userService.userDetailsService();

        UserDetails userDetails = userDetailsService.loadUserByUsername(expectedUser.getUsername());

        assertNotNull(userDetails);
        assertEquals(expectedUser, userDetails);
    }


}
