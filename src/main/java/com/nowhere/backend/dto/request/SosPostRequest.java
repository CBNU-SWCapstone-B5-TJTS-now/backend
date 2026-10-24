package com.nowhere.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SosPostRequest {

    @NotNull(message = "locationId는 필수입니다")
    private Long locationId;

    private String locationName;
    private String content;
    private Integer pointOffer; // 선택

    // 현장 인증용 현재 좌표 (장소 반경 안에서만 질문 가능)
    @NotNull(message = "latitude는 필수입니다")
    private Double latitude;

    @NotNull(message = "longitude는 필수입니다")
    private Double longitude;
}
