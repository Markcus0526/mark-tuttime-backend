package com.tuttime.user.service;

import com.tuttime.user.dto.UserDto;
import com.tuttime.user.entity.User;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** User Service: handles user registration, profile updates, and user information retrieval with caching and Kafka event publishing. */
@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
}

