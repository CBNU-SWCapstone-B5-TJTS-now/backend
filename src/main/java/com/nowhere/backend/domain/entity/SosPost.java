package com.nowhere.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sos_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SosPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 장소 ID (Location FK 없이 보관 — 게시글은 장소 삭제 후에도 이력 유지) */
    @Column(nullable = false)
    private Long locationId;

    @Column(nullable = false, length = 100)
    private String locationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    /** 요청자가 제시하는 포인트 (선택) */
    @Column
    private Integer pointOffer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SosPostStatus status = SosPostStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SosReply> replies = new ArrayList<>();

    public void accept() {
        this.status = SosPostStatus.ACCEPTED;
    }

    public void complete() {
        this.status = SosPostStatus.COMPLETED;
    }

    public void cancel() {
        this.status = SosPostStatus.CANCELLED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isOpen() {
        return this.status == SosPostStatus.OPEN;
    }
}
