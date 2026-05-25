package com.nowhere.backend.dto.request;

import com.nowhere.backend.domain.entity.LocationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlaceProposalRequest(

        @NotBlank(message = "장소명을 입력해주세요")
        @Size(max = 100, message = "장소명은 100자 이하여야 합니다")
        String name,

        @NotNull(message = "위도를 입력해주세요")
        BigDecimal latitude,

        @NotNull(message = "경도를 입력해주세요")
        BigDecimal longitude,

        @NotNull(message = "카테고리를 선택해주세요")
        LocationCategory category,

        @Size(max = 200, message = "설명은 200자 이하여야 합니다")
        String description
) {}
