package com.nowhere.backend.domain.entity;

public enum PointTransactionType {
    SOS_ESCROW,    // SOS 수락 시 요청자 포인트 차감 (에스크로 보관)
    SOS_COMPLETE,  // 거래 완료 시 응답자에게 포인트 지급
    SOS_REFUND,    // 거래 취소/만료 시 요청자에게 환불
    EARN_REPORT,    // 혼잡도 제보 적립 (향후)
    EARN_REVIEW,    // Peer Review 참여 적립 (향후)
    EARN_PROPOSAL,  // 장소 제안 승인 시 보상 지급
    PURCHASE        // 앱 내 결제로 포인트 충전 (향후)
}
