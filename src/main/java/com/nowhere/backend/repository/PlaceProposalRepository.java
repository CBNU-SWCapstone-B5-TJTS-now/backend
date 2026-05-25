package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.PlaceProposal;
import com.nowhere.backend.domain.entity.PlaceProposalStatus;
import com.nowhere.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceProposalRepository extends JpaRepository<PlaceProposal, Long> {

    List<PlaceProposal> findByStatusOrderByCreatedAtDesc(PlaceProposalStatus status);

    List<PlaceProposal> findByProposerOrderByCreatedAtDesc(User proposer);

    List<PlaceProposal> findAllByOrderByCreatedAtDesc();
}
