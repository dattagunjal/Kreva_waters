package com.mineralwater.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @Column(name = "mobile_number", unique = true)
    private String mobileNumber;

    /** Never serialised into API responses. */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column
    private String address;

    @Column(columnDefinition = "TEXT")
    private String profileImage;

    @Column(updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = java.time.LocalDateTime.now();
    }

    public enum Role { USER, ADMIN }

    /** Returns whichever identifier the user registered with — used as the JWT subject. */
    public String getPrincipal() {
        return email != null ? email : mobileNumber;
    }
}
