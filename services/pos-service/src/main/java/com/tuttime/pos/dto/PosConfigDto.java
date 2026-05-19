package com.tuttime.pos.dto;

import com.tuttime.pos.entity.enums.PosProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class PosConfigDto {

    public record PosConfigRequest(
            @NotNull
            UUID branchId,

            @NotNull
            PosProvider provider,

            @NotBlank
            String apiEndpoint,

            String apiKey,

            String webhookSecret,

            Boolean enabled
    ) {
    }
}
