package com.nowhere.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "place_proposals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PlaceProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_id", nullable = false)
    private User proposer;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationCategory category;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlaceProposalStatus status = PlaceProposalStatus.PENDING;

    @Column(length = 200)
    private String rejectionReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void approve() {
        this.status = PlaceProposalStatus.APPROVED;
    }

    public void reject(String reason) {
        this.status = PlaceProposalStatus.REJECTED;
        this.rejectionReason = reason;
    }
}
