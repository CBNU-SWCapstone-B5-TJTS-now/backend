package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.SosPost;
import com.nowhere.backend.domain.entity.SosPostStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SosPostResponse {

    private Long id;
    private Long locationId;
    private String locationName;
    private String authorNickname;
    private String content;
    private Integer pointOffer;
    private SosPostStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private List<SosReplyResponse> replies;

    public static SosPostResponse from(SosPost post) {
        return new SosPostResponse(
                post.getId(),
                post.getLocationId(),
                post.getLocationName(),
                post.getUser().getNickname(),
                post.getContent(),
                post.getPointOffer(),
                post.getStatus(),
                post.getExpiresAt(),
                post.getCreatedAt(),
                post.getReplies().stream()
                        .map(SosReplyResponse::from)
                        .collect(Collectors.toList())
        );
    }

    /** 목록용 (replies 제외) */
    public static SosPostResponse summary(SosPost post) {
        return new SosPostResponse(
                post.getId(),
                post.getLocationId(),
                post.getLocationName(),
                post.getUser().getNickname(),
                post.getContent(),
                post.getPointOffer(),
                post.getStatus(),
                post.getExpiresAt(),
                post.getCreatedAt(),
                List.of()
        );
    }
}
