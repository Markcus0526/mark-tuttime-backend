package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.AuthenticationException;
import com.tuttime.common.exception.ErrorCodes;
import com.tuttime.common.exception.ErrorResponse;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.UserResponse;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.query.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserController error handling scenarios.
 * 
 * <p>These tests verify that the controller properly throws exceptions
 * that are handled by GlobalExceptionHandler, ensuring consistent error
 * responses with proper error codes and HTTP status codes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Error Handling Tests")
class UserControllerErrorTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Create a test exception handler to capture exceptions
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("logout should throw AuthenticationException when authorization header is missing")
    void testLogout_MissingAuthorizationHeader_ThrowsAuthenticationException() {
        // Given
        String authorizationHeader = null;

        // When & Then
        assertThatThrownBy(() -> userController.logout(authorizationHeader))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authorization token is required");
    }

    @Test
    @DisplayName("logout should throw AuthenticationException when authorization header doesn't start with Bearer")
    void testLogout_InvalidAuthorizationHeader_ThrowsAuthenticationException() {
        // Given
        String authorizationHeader = "Invalid token";

        // When & Then
        assertThatThrownBy(() -> userController.logout(authorizationHeader))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authorization token is required");
    }

    @Test
    @DisplayName("logout should throw AuthenticationException when token is invalid")
    void testLogout_InvalidToken_ThrowsAuthenticationException() {
        // Given
        String authorizationHeader = "Bearer invalid-token";
        when(authService.getUserIdFromToken("invalid-token")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userController.logout(authorizationHeader))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid token");
    }

    @Test
    @DisplayName("getUserById should propagate ResourceNotFoundException when user not found")
    void testGetUserById_UserNotFound_ThrowsResourceNotFoundException() {
        // Given
        Long userId = 999L;
        when(userQueryService.getUserById(userId))
                .thenThrow(new ResourceNotFoundException("User not found with ID: " + userId));

        // When & Then
        assertThatThrownBy(() -> userController.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: " + userId);
    }

    @Test
    @DisplayName("getUserById should return success when user exists")
    void testGetUserById_UserExists_ReturnsSuccess() {
        // Given
        Long userId = 1L;
        UserResponse userResponse = UserResponse.builder()
                .userId(userId)
                .email("test@example.com")
                .build();
        when(userQueryService.getUserById(userId)).thenReturn(userResponse);

        // When
        ResponseEntity<ApiResponse<UserResponse>> response = userController.getUserById(userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(userResponse);
    }

    /**
     * Test exception handler to verify exceptions are thrown correctly.
     * In real scenarios, GlobalExceptionHandler will handle these.
     */
    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
            ErrorResponse error = ErrorResponse.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error("Unauthorized")
                    .errorCode(ErrorCodes.UNAUTHORIZED)
                    .message(ex.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
            ErrorResponse error = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .error("Resource Not Found")
                    .errorCode(ErrorCodes.RESOURCE_NOT_FOUND)
                    .message(ex.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}

