package com.seventest.infrastructure.persistence.repository;

import com.seventest.infrastructure.persistence.entity.IncorrectCorrectionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IncorrectCorrectionLogJpaRepository extends JpaRepository<IncorrectCorrectionLogEntity, UUID> {
}
