package com.miguelbf.exchangerateapi.service;

import com.miguelbf.exchangerateapi.entities.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface IUserService {

    UserDetailsService userDetailsService();

    @Nullable User getUserByEmail(String email);

    User createUser(User user);

}
