package com.seventest.domain.port.out;

import java.util.UUID;

public interface AiGradingJobDispatcher {
    void dispatch(UUID jobId);
}
