package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.LocationCategory;
import com.nowhere.backend.domain.entity.PlaceProposal;
import com.nowhere.backend.domain.entity.PlaceProposalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlaceProposalResponse(
        Long id,
        String proposerNickname,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        LocationCategory category,
        String description,
        PlaceProposalStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PlaceProposalResponse from(PlaceProposal proposal) {
        return new PlaceProposalResponse(
                proposal.getId(),
                proposal.getProposer().getNickname(),
                proposal.getName(),
                proposal.getLatitude(),
                proposal.getLongitude(),
                proposal.getCategory(),
                proposal.getDescription(),
                proposal.getStatus(),
                proposal.getRejectionReason(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
        );
    }
}
