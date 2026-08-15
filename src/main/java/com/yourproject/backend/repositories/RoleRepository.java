package com.yourproject.backend.repositories;

import com.yourproject.backend.models.Role;
import com.yourproject.backend.models.UserRole;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByRoleName(UserRole roleName);
}
