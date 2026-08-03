package com.miguelbf.exchangerateapi.service.impl;

import com.miguelbf.exchangerateapi.entities.User;
import com.miguelbf.exchangerateapi.repository.UserRepository;
import com.miguelbf.exchangerateapi.service.IUserService;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                User user = userRepository.findByEmail(username);
                if (user == null) {
                    throw new UsernameNotFoundException("User by that email not found");
                }
                return user;
            }
        };
    }

    @Override
    public @Nullable User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }
}
