# Auth Service Bug Fixes TODO

## Steps:
- [x] 1. Update DTOs: Add @Builder to AuthRequest, RegisterRequest, AuthResponse
- [x] 2. Fix SecurityConfig: Remove duplicate AuthenticationProvider field, restore proper @Bean
- [x] 3. Update JwtService: Use @Value for secret, dynamic key
- [x] 4. Update AuthService: Add email uniqueness check in register
 - [x] 5. Update AuthController: Add @Valid to request bodies
- [x] 6. Update application.yml: Use env var for JWT secret
- [x] 7. Compile and test: mvn clean compile successful
- [x] 8. Complete

**Now checking booking-service**
