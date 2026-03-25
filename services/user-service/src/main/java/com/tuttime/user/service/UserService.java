package com.tuttime.user.service;

import com.tuttime.user.dto.SignupRequest;
import com.tuttime.user.dto.UserDto;
import com.tuttime.user.entity.*;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.RoleRepository;
import com.tuttime.user.repository.UserAuthMethodRepository;
import com.tuttime.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** User Service: handles user registration, profile updates, and user information retrieval with caching and Kafka event publishing. */
@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthMethodRepository userAuthMethodRepository; // add this repo
    private final RoleRepository roleRepository;

    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    public UserDto getUserByPhoneNumber(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    public UserDto createUser(UserDto userDto) {
        User user = userMapper.toEntity(userDto);
        // Additional logic for validation, password hash, etc.
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    public UserDto updateUser(UUID id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        // Update fields as needed
        user.setFullName(userDto.getFullName());
        user.setProfilePicture(userDto.getProfilePicture());
        // etc.
        User updated = userRepository.save(user);
        return userMapper.toDto(updated);
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void signup(SignupRequest request) {
        if ((request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) &&
        (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new IllegalArgumentException("Phone number or email must be provided");
        }

        if (request.getPhoneNumber() != null && userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new EntityExistsException("Phone number already in use");
        }
        if (request.getEmail() != null && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EntityExistsException("Email already in use");
        }

        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        user = userRepository.save(user);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            // Save email/password auth method
            UserAuthMethod authMethod = UserAuthMethod.builder()
                    .user(user)
                    .authType(AuthProvider.EMAIL_PASS)
                    .credential(passwordEncoder.encode(request.getPassword()))
                    .verified(false)
                    .build();
            userAuthMethodRepository.save(authMethod);
        }

        // Assign default role GUEST
        Role userRole = roleRepository.findByName("GUEST")
                .orElseThrow(() -> new IllegalStateException("Default role GUEST not found"));
        user.getRoles().add(UserRole.builder()
                .user(user)
                .role(userRole)
                .assignedAt(java.time.Instant.now())
                .build());
        userRepository.save(user);

        // TODO: trigger SMS or email verification (via AuthService)
    }
}

