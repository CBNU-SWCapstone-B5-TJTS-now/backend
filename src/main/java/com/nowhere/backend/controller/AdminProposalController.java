package com.nowhere.backend.controller;

import com.nowhere.backend.domain.entity.PlaceProposalStatus;
import com.nowhere.backend.dto.request.ProposalApproveRequest;
import com.nowhere.backend.dto.request.ProposalRejectRequest;
import com.nowhere.backend.dto.response.PlaceProposalResponse;
import com.nowhere.backend.service.PlaceProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/proposals")
@RequiredArgsConstructor
public class AdminProposalController {

    private final PlaceProposalService proposalService;

    @GetMapping
    public ResponseEntity<List<PlaceProposalResponse>> getProposals(
            @RequestParam(required = false) PlaceProposalStatus status
    ) {
        return ResponseEntity.ok(proposalService.getAllProposals(status));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<PlaceProposalResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody ProposalApproveRequest request
    ) {
        return ResponseEntity.ok(proposalService.approve(id, request));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<PlaceProposalResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody ProposalRejectRequest request
    ) {
        return ResponseEntity.ok(proposalService.reject(id, request));
    }
}
