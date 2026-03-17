package com.tuttime.authservice.controller;

import com.tuttime.authservice.dto.AuthRequest;
import com.tuttime.authservice.dto.AuthResponse;
import com.tuttime.authservice.dto.RegisterRequest;
import com.tuttime.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

@PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }

  @PostMapping("/authenticate")
  public ResponseEntity<AuthResponse> authenticate(@RequestBody @Valid AuthRequest request) {
    return ResponseEntity.ok(authService.authenticate(request));
  }
}
