package com.yourproject.backend.services;

import java.util.List;

import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.UpdateUserRequest;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

public interface UserService {
    User createUser(CreateUserRequest request, String createdBy);

    User getUserById(String userId);

    User getActiveUserById(String userId);

    User findByPhoneNumberAndRole(String phoneNumber, UserRole role);

    List<User> getAllUsers();

    User updateUser(String userId, UpdateUserRequest request, String updatedBy);

    User toggleUserStatus(String userId, String requestedBy);

    void changePassword(String userId, ChangePasswordRequest request);

    void recordSuccessfulLogin(User user);
}
