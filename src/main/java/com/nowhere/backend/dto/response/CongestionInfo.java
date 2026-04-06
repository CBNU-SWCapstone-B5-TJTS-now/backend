package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.CongestionLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CongestionInfo {
    private CongestionLevel level;
    private int approvalCount;
    private int rejectionCount;
    private LocalDateTime updatedAt;
}
