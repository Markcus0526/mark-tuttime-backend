package com.tuttime.restaurant.dto;

import lombok.Data;

@Data
public class AuthResponse {
    
    private Long id;
    
    private String token;
    
    private boolean success;
    
    private String message;
    
}