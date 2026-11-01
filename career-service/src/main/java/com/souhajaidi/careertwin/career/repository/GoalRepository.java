package com.souhajaidi.careertwin.career.repository;

import com.souhajaidi.careertwin.career.domain.Goal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    Optional<Goal> findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    List<Goal> findByUserIdAndActiveTrue(UUID userId);
}
