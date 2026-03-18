package com.tuttime.authservice.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AuthRequest {
  private String email;
  private String password;
}
