package com.nowhere.backend.controller;

import com.nowhere.backend.dto.request.PlaceProposalRequest;
import com.nowhere.backend.dto.response.PlaceProposalResponse;
import com.nowhere.backend.service.PlaceProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class PlaceProposalController {

    private final PlaceProposalService proposalService;

    @PostMapping
    public ResponseEntity<PlaceProposalResponse> propose(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody PlaceProposalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.propose(email, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<PlaceProposalResponse>> getMyProposals(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(proposalService.getMyProposals(email));
    }
}
