package com.seventest.infrastructure.persistence.adapter;

import com.seventest.domain.model.PasswordPolicy;
import com.seventest.domain.port.out.PasswordPolicyRepository;
import com.seventest.infrastructure.persistence.mapper.PasswordPolicyMapper;
import com.seventest.infrastructure.persistence.repository.PasswordPolicyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordPolicyRepositoryAdapter implements PasswordPolicyRepository {

    private final PasswordPolicyJpaRepository jpaRepository;

    @Override
    public Optional<PasswordPolicy> find() {
        return jpaRepository.findById(1L).map(PasswordPolicyMapper::toDomain);
    }

    @Override
    public PasswordPolicy save(PasswordPolicy policy) {
        var entity = jpaRepository.save(PasswordPolicyMapper.toEntity(policy));
        return PasswordPolicyMapper.toDomain(entity);
    }
}
