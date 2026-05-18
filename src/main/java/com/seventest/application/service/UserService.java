package com.seventest.application.service;

import com.seventest.domain.exception.EmailAlreadyExistsException;
import com.seventest.domain.exception.PasswordPolicyViolationException;
import com.seventest.domain.exception.UserNotFoundException;
import com.seventest.domain.model.*;
import com.seventest.domain.port.in.UserManagementUseCase;
import com.seventest.domain.port.out.PasswordPolicyRepository;
import com.seventest.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserManagementUseCase {

    private final UserRepository userRepository;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User create(String fullName, String email, Role role, String initialPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        PasswordPolicy policy = passwordPolicyRepository.find()
                .orElse(PasswordPolicyService.DEFAULT);
        validatePassword(initialPassword, policy);

        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName(fullName)
                .email(email)
                .role(role)
                .status(UserStatus.ACTIVO)
                .passwordHash(passwordEncoder.encode(initialPassword))
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User update(UUID id, String fullName, String email, Role role, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (!user.getEmail().equals(email) && userRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailAlreadyExistsException(email);
        }

        String passwordHash = user.getPasswordHash();
        if (newPassword != null && !newPassword.isBlank()) {
            PasswordPolicy policy = passwordPolicyRepository.find().orElse(PasswordPolicyService.DEFAULT);
            validatePassword(newPassword, policy);
            passwordHash = passwordEncoder.encode(newPassword);
        }

        return userRepository.save(user.toBuilder()
                .fullName(fullName)
                .email(email)
                .role(role)
                .passwordHash(passwordHash)
                .build());
    }

    @Override
    public void deactivate(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        userRepository.save(user.toBuilder().status(UserStatus.INACTIVO).build());
    }

    @Override
    public void reactivate(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        userRepository.save(user.toBuilder().status(UserStatus.ACTIVO).build());
    }

    @Override
    public PageResult<User> list(String search, Role roleFilter, UserStatus statusFilter, int page, int size) {
        return userRepository.findAll(search, roleFilter, statusFilter, page, size);
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void validatePassword(String password, PasswordPolicy policy) {
        if (password.length() < policy.getMinLength()) {
            throw new PasswordPolicyViolationException(
                    "La contraseña debe tener al menos " + policy.getMinLength() + " caracteres");
        }
        if (password.length() > policy.getMaxLength()) {
            throw new PasswordPolicyViolationException(
                    "La contraseña no puede superar " + policy.getMaxLength() + " caracteres");
        }
        if (policy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            throw new PasswordPolicyViolationException("La contraseña debe contener al menos una mayúscula");
        }
        if (policy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            throw new PasswordPolicyViolationException("La contraseña debe contener al menos una minúscula");
        }
        if (policy.isRequireNumbers() && password.chars().noneMatch(Character::isDigit)) {
            throw new PasswordPolicyViolationException("La contraseña debe contener al menos un número");
        }
        if (policy.isRequireSpecialChars() && password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new PasswordPolicyViolationException("La contraseña debe contener al menos un carácter especial");
        }
    }
}
