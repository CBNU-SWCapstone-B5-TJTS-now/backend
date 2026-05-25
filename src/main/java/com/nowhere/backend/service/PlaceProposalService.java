package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.*;
import com.nowhere.backend.dto.request.PlaceProposalRequest;
import com.nowhere.backend.dto.request.ProposalApproveRequest;
import com.nowhere.backend.dto.request.ProposalRejectRequest;
import com.nowhere.backend.dto.response.PlaceProposalResponse;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.repository.LocationRepository;
import com.nowhere.backend.repository.PlaceProposalRepository;
import com.nowhere.backend.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceProposalService {

    private static final int PROPOSAL_REWARD_POINTS = 100;

    private final PlaceProposalRepository proposalRepository;
    private final LocationRepository locationRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserService userService;

    @Transactional
    public PlaceProposalResponse propose(String email, PlaceProposalRequest request) {
        User proposer = userService.findByEmail(email);

        PlaceProposal proposal = PlaceProposal.builder()
                .proposer(proposer)
                .name(request.name())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .category(request.category())
                .description(request.description())
                .build();

        return PlaceProposalResponse.from(proposalRepository.save(proposal));
    }

    @Transactional(readOnly = true)
    public List<PlaceProposalResponse> getMyProposals(String email) {
        User user = userService.findByEmail(email);
        return proposalRepository.findByProposerOrderByCreatedAtDesc(user)
                .stream()
                .map(PlaceProposalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaceProposalResponse> getAllProposals(PlaceProposalStatus status) {
        List<PlaceProposal> proposals = (status != null)
                ? proposalRepository.findByStatusOrderByCreatedAtDesc(status)
                : proposalRepository.findAllByOrderByCreatedAtDesc();

        return proposals.stream()
                .map(PlaceProposalResponse::from)
                .toList();
    }

    @Transactional
    public PlaceProposalResponse approve(Long proposalId, ProposalApproveRequest request) {
        PlaceProposal proposal = findProposalById(proposalId);

        if (proposal.getStatus() != PlaceProposalStatus.PENDING) {
            throw new BusinessException("이미 처리된 제안입니다", HttpStatus.CONFLICT);
        }

        proposal.approve();

        Location newLocation = Location.builder()
                .name(proposal.getName())
                .latitude(proposal.getLatitude())
                .longitude(proposal.getLongitude())
                .category(proposal.getCategory())
                .geofenceRadius(request.geofenceRadius())
                .defaultTtlMinutes(request.defaultTtlMinutes())
                .maxTtlMinutes(request.maxTtlMinutes())
                .build();
        locationRepository.save(newLocation);

        rewardProposer(proposal.getProposer());

        return PlaceProposalResponse.from(proposal);
    }

    @Transactional
    public PlaceProposalResponse reject(Long proposalId, ProposalRejectRequest request) {
        PlaceProposal proposal = findProposalById(proposalId);

        if (proposal.getStatus() != PlaceProposalStatus.PENDING) {
            throw new BusinessException("이미 처리된 제안입니다", HttpStatus.CONFLICT);
        }

        proposal.reject(request.reason());
        return PlaceProposalResponse.from(proposal);
    }

    private PlaceProposal findProposalById(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 제안입니다", HttpStatus.NOT_FOUND));
    }

    private void rewardProposer(User proposer) {
        proposer.addPoint(PROPOSAL_REWARD_POINTS);

        PointTransaction reward = PointTransaction.builder()
                .user(proposer)
                .type(PointTransactionType.EARN_PROPOSAL)
                .amount(PROPOSAL_REWARD_POINTS)
                .description("장소 제안 승인 보상")
                .build();
        pointTransactionRepository.save(reward);
    }
}
