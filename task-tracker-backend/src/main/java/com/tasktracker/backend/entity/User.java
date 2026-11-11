package com.tasktracker.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends UuidIdentified {

    @Column(nullable = false, length = 254)
    private String email;

    /** Хеш пароля (BCrypt), не сам пароль. */
    @Column(nullable = false, length = 100)
    private String password;

    /** Заполняется базой при вставке и сразу перечитывается. */
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
