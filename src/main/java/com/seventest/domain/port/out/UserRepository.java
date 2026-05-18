package com.seventest.domain.port.out;

import com.seventest.domain.model.PageResult;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    PageResult<User> findAll(String search, Role role, UserStatus status, int page, int size);
}
