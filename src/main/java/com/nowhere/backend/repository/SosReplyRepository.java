package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.SosPost;
import com.nowhere.backend.domain.entity.SosReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SosReplyRepository extends JpaRepository<SosReply, Long> {

    List<SosReply> findByPostOrderByCreatedAtAsc(SosPost post);

    Optional<SosReply> findByIdAndPost(Long id, SosPost post);
}
