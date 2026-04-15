package com.tuttime.user.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
		throw new InvalidRequestException("Load by username not supported");
	}

}