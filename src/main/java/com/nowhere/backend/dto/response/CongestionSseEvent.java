package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.CongestionLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CongestionSseEvent {

    private Long locationId;
    private CongestionLevel congestionLevel;
    private int approvalCount;
    private LocalDateTime updatedAt;
}
