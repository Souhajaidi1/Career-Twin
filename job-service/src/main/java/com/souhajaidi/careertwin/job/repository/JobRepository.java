package com.souhajaidi.careertwin.job.repository;

import com.souhajaidi.careertwin.job.domain.Job;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Job> findByIdAndUserId(UUID id, UUID userId);
}
