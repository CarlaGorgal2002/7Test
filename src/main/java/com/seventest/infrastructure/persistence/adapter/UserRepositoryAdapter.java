package com.seventest.infrastructure.persistence.adapter;

import com.seventest.domain.model.PageResult;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;
import com.seventest.domain.port.out.UserRepository;
import com.seventest.infrastructure.config.HardcodedUsers;
import com.seventest.infrastructure.persistence.UserSpecification;
import com.seventest.infrastructure.persistence.entity.UserEntity;
import com.seventest.infrastructure.persistence.mapper.UserMapper;
import com.seventest.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final HardcodedUsers hardcodedUsers;

    @Override
    public User save(User user) {
        if (hardcodedUsers.containsId(user.getId()) || hardcodedUsers.containsEmail(user.getEmail())) {
            return hardcodedUsers.findById(user.getId())
                    .or(() -> hardcodedUsers.findByEmail(user.getEmail()))
                    .orElse(user);
        }

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
        return hardcodedUsers.findById(id)
                .or(() -> jpaRepository.findById(id).map(UserMapper::toDomain));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return hardcodedUsers.findByEmail(email)
                .or(() -> jpaRepository.findByEmail(email).map(UserMapper::toDomain));
    }

    @Override
    public Optional<User> findByFullName(String fullName) {
        return hardcodedUsers.findByFullName(fullName)
                .or(() -> jpaRepository.findByFullNameIgnoreCase(fullName).map(UserMapper::toDomain));
    }

    @Override
    public boolean existsByEmail(String email) {
        return hardcodedUsers.containsEmail(email) || jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return hardcodedUsers.findByEmail(email)
                .map(user -> !user.getId().equals(id))
                .orElse(false)
                || jpaRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public PageResult<User> findAll(String search, Role role, UserStatus status, int page, int size) {
        List<User> databaseUsers = jpaRepository.findAll(
                UserSpecification.withFilters(search, role, status),
                Sort.by("fullName").ascending()
        ).stream()
                .map(UserMapper::toDomain)
                .filter(user -> !hardcodedUsers.containsId(user.getId()))
                .filter(user -> !hardcodedUsers.containsEmail(user.getEmail()))
                .toList();

        List<User> merged = Stream.concat(
                        hardcodedUsers.all().stream().filter(user -> matches(user, search, role, status)),
                        databaseUsers.stream())
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int from = Math.min(safePage * safeSize, merged.size());
        int to = Math.min(from + safeSize, merged.size());
        int totalPages = (int) Math.ceil((double) merged.size() / safeSize);

        return new PageResult<>(merged.subList(from, to), merged.size(), totalPages, safePage);
    }

    private boolean matches(User user, String search, Role role, UserStatus status) {
        if (role != null && user.getRole() != role) {
            return false;
        }
        if (status != null && user.getStatus() != status) {
            return false;
        }
        if (search == null || search.isBlank()) {
            return true;
        }

        String normalizedSearch = search.trim().toLowerCase(Locale.ROOT);
        return user.getFullName().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || user.getEmail().toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }
}
