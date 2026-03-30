package com.tuttime.user.service.impl;

import com.tuttime.user.entity.User;
import com.tuttime.user.enums.UserStatus;
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
		throw new UsernameNotFoundException("Load by username not supported");
	}

	/**
	 * Loads UserDetails by user UUID string.
	 * Used in JWT filter to authenticate requests.
	 *
	 * @param userIdString the UUID string of the user.
	 * @return UserDetails object with roles as authorities.
	 */
	public UserDetails loadUserById(String userIdString) {
		UUID userId = UUID.fromString(userIdString);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with id " + userId));

		Set<GrantedAuthority> authorities = user.getRoles().stream()
				.map(userRole -> userRole.getRole().getName())
				.map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
				.collect(Collectors.toSet());

		return new org.springframework.security.core.userdetails.User(
				user.getId().toString(),
				"", // password not needed here
				!user.isDeleted(),
				true,
				true,
				user.getStatus() == UserStatus.ACTIVE,
				authorities);
	}
}