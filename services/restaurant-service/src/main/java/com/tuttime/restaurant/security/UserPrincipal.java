package com.tuttime.restaurant.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public record UserPrincipal(
		UUID id,
		String username,
		Collection<? extends GrantedAuthority> authorities
) implements UserDetails {
	@Override public String getPassword() { return null; }
	@Override public String getUsername() { return username; }
	@Override public boolean isAccountNonExpired() { return true; }
	@Override public boolean isAccountNonLocked() { return true; }
	@Override public boolean isCredentialsNonExpired() { return true; }
	@Override public boolean isEnabled() { return true; }
	@Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
}