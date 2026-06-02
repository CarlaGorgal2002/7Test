package com.seventest.infrastructure.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExtraTimeRequest(
        @NotNull @Min(1) @Max(60) Integer extraMinutes
) {
}
