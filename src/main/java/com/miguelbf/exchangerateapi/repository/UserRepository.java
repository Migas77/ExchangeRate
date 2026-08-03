package com.miguelbf.exchangerateapi.repository;

import com.miguelbf.exchangerateapi.entities.User;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Nullable User findByEmail(String email);

}
