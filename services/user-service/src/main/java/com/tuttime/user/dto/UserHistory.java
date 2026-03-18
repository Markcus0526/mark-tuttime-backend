package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * User history DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHistory {
    private List<ConversionHistoryItem> conversions;
    private List<TransactionHistoryItem> transactions;
    private List<BoostHistoryItem> boosts;
}

