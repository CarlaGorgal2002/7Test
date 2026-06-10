package com.seventest.domain.model;

public record AiGradingStatus(
        boolean enabled,
        boolean available,
        String model,
        String materialVersion,
        String promptVersion
) {
}
