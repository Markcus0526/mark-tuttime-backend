package com.tuttime.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

//    @Mock
//    private AuthService authService;
//
//    @Mock
//    private SmsVerificationService smsVerificationService;
//
//    @Mock
//    private UserService userService;
//
//    @Mock
//    private AntiFraudService antiFraudService;
//
//    @Mock
//    private HttpServletRequest httpRequest;
//
//    @InjectMocks
//    private AuthController authController;
//
//    @BeforeEach
//    void setUp() {
//        // Setup common mocks
//        lenient().when(httpRequest.getHeader("X-Device-Id")).thenReturn("device-123");
//        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
//        lenient().when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
//    }
//
//    @Test
//    @DisplayName("Should send SMS verification code successfully")
//    void testSendSignupSms_Success_ReturnsOk() {
//        // Given
//        PhoneNumberRequest request = new PhoneNumberRequest();
//        request.setPhoneNumber("+1234567890");
//
//        when(userService.isPhoneRegistered(anyString())).thenReturn(false);
//        when(smsVerificationService.sendSignupSms(anyString(), anyString(), any())).thenReturn("SMS");
//
//        // When
//        ResponseEntity<ApiResponse<MessageResponse>> response =
//                authController.sendSignupSms(request, httpRequest);
//
//        // Then
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getSuccess()).isTrue();
//        verify(smsVerificationService).sendSignupSms(anyString(), anyString(), any());
//    }
//
//    @Test
//    @DisplayName("Should verify SMS code and return signup token")
//    void testVerifySignupSms_Success_ReturnsSignupToken() {
//        // Given
//        VerifySmsRequest request = new VerifySmsRequest();
//        request.setPhoneNumber("+1234567890");
//        request.setSmsCode("123456");
//
//        String signupToken = "signup-token-123";
//        when(smsVerificationService.verifySmsCode(anyString(), anyString())).thenReturn(signupToken);
//
//        // When
//        ResponseEntity<ApiResponse<SignupTokenResponse>> response =
//                authController.verifySignupSms(request);
//
//        // Then
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getSuccess()).isTrue();
//        assertThat(response.getBody().getData().getSignupToken()).isEqualTo(signupToken);
//        verify(smsVerificationService).verifySmsCode(anyString(), anyString());
//    }
//
//    @Test
//    @DisplayName("Should complete signup successfully")
//    void testSignup_Success_ReturnsCreated() {
//        // Given
//        SignupRequest request = new SignupRequest();
//        request.setPhoneNumber("+1234567890");
//        request.setEmail("test@example.com");
//        request.setPassword("password123");
//        request.setFirstName("John");
//        request.setLastName("Doe");
//        request.setSignUpToken("signup-token-123");
//
//        UserResponse userResponse = UserResponse.builder()
//                .userId(1L)
//                .email("test@example.com")
//                .build();
//
//        LoginResponse loginResponse = LoginResponse.builder()
//                .token("access-token")
//                .refreshToken("refresh-token")
//                .build();
//
//        when(smsVerificationService.validateSignupToken(anyString())).thenReturn("+1234567890");
//        doNothing().when(antiFraudService).validateDeviceRegistration(anyString(), anyString(), anyLong());
//        when(userService.registerUser(any())).thenReturn(userResponse);
//        doNothing().when(smsVerificationService).invalidateSignupToken(anyString());
//        when(authService.login(any())).thenReturn(loginResponse);
//
//        // When
//        ResponseEntity<ApiResponse<LoginResponse>> response =
//                authController.signup(request, httpRequest);
//
//        // Then
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getSuccess()).isTrue();
//        verify(userService).registerUser(any());
//        verify(authService).login(any());
//    }
//
//    @Test
//    @DisplayName("Should sign in successfully")
//    void testSignin_Success_ReturnsOk() {
//        // Given
//        SigninRequest request = new SigninRequest();
//        request.setPhoneNumber("+1234567890");
//        request.setPassword("password123");
//
//        LoginResponse loginResponse = LoginResponse.builder()
//                .token("access-token")
//                .refreshToken("refresh-token")
//                .build();
//
//        when(authService.signinWithPhone(anyString(), anyString())).thenReturn(loginResponse);
//
//        // When
//        ResponseEntity<ApiResponse<AuthTokens>> response =
//                authController.signin(request, httpRequest);
//
//        // Then
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getSuccess()).isTrue();
//        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("access-token");
//        verify(authService).signinWithPhone(anyString(), anyString());
//    }
//
//    @Test
//    @DisplayName("Should refresh token successfully")
//    void testRefresh_Success_ReturnsOk() {
//        // Given
//        RefreshTokenRequest request = new RefreshTokenRequest();
//        request.setRefreshToken("refresh-token-123");
//
//        LoginResponse loginResponse = LoginResponse.builder()
//                .token("new-access-token")
//                .refreshToken("new-refresh-token")
//                .build();
//
//        when(authService.refreshToken(anyString())).thenReturn(loginResponse);
//
//        // When
//        ResponseEntity<ApiResponse<AuthTokens>> response =
//                authController.refresh(request);
//
//        // Then
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getSuccess()).isTrue();
//        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("new-access-token");
//        verify(authService).refreshToken(anyString());
//    }
//
//    @Test
//    @DisplayName("Should sign out successfully")
//    void testSignout_Success_ReturnsOk() {
//        // Given
//        String authHeader = "Bearer access-token-123";
//        Long userId = 1L;
//
//        when(authService.getUserIdFromToken(anyString())).thenReturn(userId);
//        doNothing().when(authService).logout(anyString(), anyLong());
//
//        // When
//        ResponseEntity<ApiResponse<String>> response =
//                authController.signout(authHeader);
//
//        // Then
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getSuccess()).isTrue();
//        verify(authService).logout(anyString(), anyLong());
//    }
}
