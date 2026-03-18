package com.tuttime.authservice.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class RegisterRequest {
  private String email;
  private String password;
  @Builder.Default
  private String role = "USER";
}
