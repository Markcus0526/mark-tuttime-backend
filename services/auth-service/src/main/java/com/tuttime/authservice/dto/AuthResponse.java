package com.tuttime.authservice.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AuthResponse {
  private String token;
}
