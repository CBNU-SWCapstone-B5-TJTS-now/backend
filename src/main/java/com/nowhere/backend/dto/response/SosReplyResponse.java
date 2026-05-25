package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.SosReply;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SosReplyResponse {

    private Long id;
    private Long postId;
    private String authorNickname;
    private String content;
    private boolean accepted;
    private boolean completed;
    private LocalDateTime createdAt;

    public static SosReplyResponse from(SosReply reply) {
        return new SosReplyResponse(
                reply.getId(),
                reply.getPost().getId(),
                reply.getUser().getNickname(),
                reply.getContent(),
                reply.isAccepted(),
                reply.isCompleted(),
                reply.getCreatedAt()
        );
    }
}
