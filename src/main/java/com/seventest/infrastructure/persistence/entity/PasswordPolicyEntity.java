package com.seventest.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "password_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordPolicyEntity {

    // Fila singleton: siempre id = 1
    @Id
    private Long id;

    @Column(name = "min_length", nullable = false)
    private int minLength;

    @Column(name = "max_length", nullable = false)
    private int maxLength;

    @Column(name = "require_uppercase", nullable = false)
    private boolean requireUppercase;

    @Column(name = "require_lowercase", nullable = false)
    private boolean requireLowercase;

    @Column(name = "require_numbers", nullable = false)
    private boolean requireNumbers;

    @Column(name = "require_special_chars", nullable = false)
    private boolean requireSpecialChars;
}
