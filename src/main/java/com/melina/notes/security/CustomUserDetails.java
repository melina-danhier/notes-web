package com.melina.notes.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.melina.notes.entity.User;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails (
        long id,
        String email,
        String password,
        String displayName,
        String role
) implements UserDetails {

    public CustomUserDetails(User user) {
        this(user.getId(), user.getEmail(), user.getPassword(), user.getDisplayName(), user.getRole());
    }

    public CustomUserDetails(long id, String email, String password, String displayName, String role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

}