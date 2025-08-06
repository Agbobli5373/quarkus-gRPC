package org.isaac.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a new user via REST API.
 * Contains validation annotations to ensure data integrity.
 */
public class CreateUserDto {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    // Default constructor for JSON deserialization
    public CreateUserDto() {
    }

    // Constructor with all fields
    public CreateUserDto(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and setters
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

    @Override
    public String toString() {
        return "CreateUserDto{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}