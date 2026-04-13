package com.tuttime.user.service.impl;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	/**
	 * Loads user by username (email or phone). Not used in JWT flow here.
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// Optional: Implement if needed for form login
		throw new ResourceNotFoundException("Load by username not supported");
	}

}