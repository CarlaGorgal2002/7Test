package com.seventest.infrastructure.persistence.adapter;

import com.seventest.domain.model.PageResult;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.out.UserRepository;
import com.seventest.infrastructure.persistence.UserSpecification;
import com.seventest.infrastructure.persistence.entity.UserEntity;
import com.seventest.infrastructure.persistence.mapper.UserMapper;
import com.seventest.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = jpaRepository.findById(user.getId())
                .map(existing -> {
                    existing.setFullName(user.getFullName());
                    existing.setEmail(user.getEmail());
                    existing.setRole(user.getRole());
                    existing.setStatus(user.getStatus());
                    existing.setPasswordHash(user.getPasswordHash());
                    existing.setFailedLoginAttempts(user.getFailedLoginAttempts());
                    existing.setLockedUntil(user.getLockedUntil());
                    return existing;
                })
                .orElseGet(() -> UserMapper.toEntity(user));
        return UserMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByFullName(String fullName) {
        return jpaRepository.findByFullNameIgnoreCase(fullName).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return jpaRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public PageResult<User> findAll(String search, Role role, UserStatus status, int page, int size) {
        Page<UserEntity> result = jpaRepository.findAll(
                UserSpecification.withFilters(search, role, status),
                PageRequest.of(page, size, Sort.by("fullName").ascending())
        );
        List<User> content = result.getContent().stream().map(UserMapper::toDomain).toList();
        return new PageResult<>(content, result.getTotalElements(), result.getTotalPages(), page);
    }
}
