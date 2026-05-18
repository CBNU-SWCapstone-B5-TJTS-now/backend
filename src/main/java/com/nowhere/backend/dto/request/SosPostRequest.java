package com.nowhere.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SosPostRequest {
    private Long locationId;
    private String locationName;
    private String content;
    private Integer pointOffer; // 선택
}
