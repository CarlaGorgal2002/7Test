package com.seventest.domain.port.in;

import com.seventest.domain.model.PageResult;
import com.seventest.domain.model.Role;
import com.seventest.domain.model.User;
import com.seventest.domain.model.UserStatus;

import java.util.UUID;

public interface UserManagementUseCase {
    User create(String fullName, String email, Role role, String initialPassword);
    User update(UUID id, String fullName, String email, Role role, String newPassword);
    void deactivate(UUID id);
    void reactivate(UUID id);
    PageResult<User> list(String search, Role roleFilter, UserStatus statusFilter, int page, int size);
    User findById(UUID id);
}
