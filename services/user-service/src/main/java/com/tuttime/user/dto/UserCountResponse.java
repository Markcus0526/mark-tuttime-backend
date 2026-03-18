package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Count Response DTO
 * Contains total number of users and online users count
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCountResponse {
    private Long totalUsers;
    private Long onlineUsers;
}
