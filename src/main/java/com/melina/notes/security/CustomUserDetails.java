package com.melina.notes.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.melina.notes.entity.User;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

    public CustomUserDetails(long id, String mail, String password, String role) {
        this(User.builder()
                .id(id)
                .username(mail)
                .email(mail)
                .password(password)
                .role(role)
                .build());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole()));
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public String getDisplayName() {
        return user.getUsername();
    }

}