package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.LocationSubscription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long locationId;
    private String locationName;
    private LocalDateTime subscribedAt;

    public static SubscriptionResponse from(LocationSubscription sub) {
        return new SubscriptionResponse(
                sub.getLocation().getId(),
                sub.getLocation().getName(),
                sub.getCreatedAt()
        );
    }
}
