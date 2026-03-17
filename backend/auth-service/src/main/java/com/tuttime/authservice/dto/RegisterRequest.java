package com.tuttime.authservice.dto;

import lombok.Data;

@Data
public class RegisterRequest {
  private String email;
  private String password;
  private String role = "USER";
}
