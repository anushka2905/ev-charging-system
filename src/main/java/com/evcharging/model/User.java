package com.evcharging.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

/**
 * User entity — Phase 0 enhanced version.
 *
 * Changes from v1:
 *  - Added Lombok annotations (clean, no manual getters/setters)
 *  - Added phone, firstName, lastName fields (multi-city support)
 *  - Added createdAt / updatedAt audit fields
 *  - Added enabled flag (account activation)
 *  - Role is now an enum (type-safe)
 */
@Entity
@Table(name = "users",
       indexes = {
           @Index(name = "idx_user_email",    columnList = "email",    unique = true),
           @Index(name = "idx_user_username", columnList = "username", unique = true)
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    /** "ROLE_USER" or "ROLE_ADMIN" — kept as String for backward compat */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "ROLE_USER";

    // ── Phase 0 additions ────────────────────────────────────────

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(length = 15)
    private String phone;

    /** City the user registered from — used for smart recommendations */
    @Column(length = 100)
    private String city;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Convenience: full name */
    public String getFullName() {
        if (firstName != null && lastName != null) return firstName + " " + lastName;
        if (firstName != null) return firstName;
        return username;
    }
}
