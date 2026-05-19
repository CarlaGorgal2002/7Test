package com.seventest.infrastructure.persistence.repository;

import com.seventest.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByFullNameIgnoreCase(String fullName);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
}
