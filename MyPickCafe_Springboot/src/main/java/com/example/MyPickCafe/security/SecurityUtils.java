package com.example.MyPickCafe.security;

import com.example.MyPickCafe.domain.RoleKind;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class SecurityUtils {
    public static Collection<? extends GrantedAuthority> toAuthorities(RoleKind roleKind) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleKind.name()));
    }
}
