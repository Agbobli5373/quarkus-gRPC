package org.isaac.dto;

import java.time.Instant;

/**
 * Data Transfer Object for User entity used in REST API responses.
 * This represents the complete user information returned to REST clients.
 */
public class UserDto {
    private String id;
    private String name;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;

    // Default constructor for JSON deserialization
    public UserDto() {
    }

    // Constructor with all fields
    public UserDto(String id, String name, String email, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}