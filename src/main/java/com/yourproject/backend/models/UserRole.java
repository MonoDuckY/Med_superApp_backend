package com.yourproject.backend.models;

public enum UserRole {
    ADMIN("1"),
    DOCTOR("2"),
    STAFF("3"),
    RESEARCHER("4"),
    PATIENT("5");

    private final String id;

    UserRole(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static UserRole fromId(String id) {
        for (UserRole role : values()) {
            if (role.id.equals(id)) {
                return role;
            }
        }
        return UserRole.valueOf(id);
    }
}
