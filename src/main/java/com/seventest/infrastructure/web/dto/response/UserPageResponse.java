package com.seventest.infrastructure.web.dto.response;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> content,
        long totalElements,
        int totalPages,
        int page
) {}
