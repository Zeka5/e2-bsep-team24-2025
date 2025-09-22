package com.example.bsep_backend.domain;

public enum UserRole {
    USER,
    ADMIN;

    public static UserRole fromString(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Valid roles are: " + UserRole.validRoles() + ".");
        }
    }

    public static String validRoles() {
        StringBuilder validRoles = new StringBuilder();
        for (UserRole role : UserRole.values()) {
            if (!validRoles.isEmpty()) {
                validRoles.append(", ");
            }
            validRoles.append(role.name());
        }
        return validRoles.toString();
    }
}
