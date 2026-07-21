package com.yourproject.backend.services;

import java.util.List;

import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.UpdateUserRequest;
import com.yourproject.backend.models.User;

public interface UserService {
    User createUser(CreateUserRequest request, String createdBy);

    User getUserById(String userId);

    User getActiveUserById(String userId);

    User findByUsername(String username);

    List<User> getAllUsers();

    User updateUser(String userId, UpdateUserRequest request, String updatedBy);

    void deactivateUser(String userId, String requestedBy);

    void changePassword(String userId, ChangePasswordRequest request);

    void recordSuccessfulLogin(User user);
}
