package com.seventest.infrastructure.persistence.repository;

import com.seventest.infrastructure.persistence.entity.PasswordPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordPolicyJpaRepository extends JpaRepository<PasswordPolicyEntity, Long> {}
