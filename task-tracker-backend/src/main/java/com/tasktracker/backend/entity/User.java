package com.tasktracker.backend.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("users")
public class User extends UuidIdentified {

    @Column("email")
    private String email;

    /** Хеш пароля (BCrypt), не сам пароль. */
    @Column("password")
    private String password;

    /** Заполняется аудитом Spring Data JDBC при вставке. */
    @CreatedDate
    @Column("created_at")
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
