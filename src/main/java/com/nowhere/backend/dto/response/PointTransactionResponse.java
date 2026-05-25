package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.PointTransaction;
import com.nowhere.backend.domain.entity.PointTransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionResponse {

    private Long id;
    private PointTransactionType type;
    private int amount;
    private Long sosPostId;
    private String description;
    private LocalDateTime createdAt;

    public static PointTransactionResponse from(PointTransaction tx) {
        return new PointTransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getAmount(),
                tx.getSosPostId(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
