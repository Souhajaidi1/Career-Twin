package com.souhajaidi.careertwin.career.repository;

import com.souhajaidi.careertwin.career.domain.EvidenceRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<EvidenceRecord, UUID> {

    List<EvidenceRecord> findByUserIdOrderByDateDescIdAsc(UUID userId);

    /** Every lookup is scoped to the owner, so another user's id simply does not exist. */
    Optional<EvidenceRecord> findByIdAndUserId(UUID id, UUID userId);
}
