package com.nowhere.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SosReplyRequest {

    private String content;

    // 현장 인증용 현재 좌표 (게시글 장소 반경 안에서만 답변 가능)
    @NotNull(message = "latitude는 필수입니다")
    private Double latitude;

    @NotNull(message = "longitude는 필수입니다")
    private Double longitude;
}
