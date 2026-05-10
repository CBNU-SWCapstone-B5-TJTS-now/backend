package com.nowhere.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "congestion_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CongestionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CongestionLevel congestionLevel;

    @Builder.Default
    @Column(nullable = false)
    private int approvalCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int rejectionCount = 0;

    // null이면 장소 기본값(defaultTtlMinutes) 사용
    private Integer customTtlMinutes;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean trustScoreProcessed = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void incrementApprovalCount() {
        this.approvalCount++;
    }

    public void incrementRejectionCount() {
        this.rejectionCount++;
    }

    public void markTrustScoreProcessed() {
        this.trustScoreProcessed = true;
    }
}
