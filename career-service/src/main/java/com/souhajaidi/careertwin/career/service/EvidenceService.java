package com.souhajaidi.careertwin.career.service;

import com.souhajaidi.careertwin.career.api.dto.Dtos.EvidenceRequest;
import com.souhajaidi.careertwin.career.api.dto.Dtos.EvidenceResponse;
import com.souhajaidi.careertwin.career.api.dto.Dtos.SkillRefBody;
import com.souhajaidi.careertwin.career.domain.EvidenceRecord;
import com.souhajaidi.careertwin.career.domain.EvidenceRecord.SkillRef;
import com.souhajaidi.careertwin.career.repository.EvidenceRepository;
import com.souhajaidi.careertwin.engine.Catalog;
import com.souhajaidi.careertwin.engine.EvidenceType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceService {

    /** Types the engine treats as measured inside the product; a user may not claim them by hand. */
    static final Set<EvidenceType> RESERVED = Set.of(EvidenceType.ASSESSMENT, EvidenceType.INTERVIEW);

    private final EvidenceRepository evidence;

    public EvidenceService(EvidenceRepository evidence) {
        this.evidence = evidence;
    }

    public static boolean userCreatable(EvidenceType type) {
        return !RESERVED.contains(type);
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> list(UUID userId) {
        return evidence.findByUserIdOrderByDateDescIdAsc(userId).stream()
                .map(EvidenceResponse::from)
                .toList();
    }

    @Transactional
    public EvidenceResponse create(UUID userId, EvidenceRequest request) {
        EvidenceType type = EvidenceType.fromWire(request.type());
        if (!userCreatable(type)) {
            throw CareerException.reservedType(request.type());
        }
        // Origin is a property of the evidence type, never of the request: a client cannot
        // upgrade its own claim to "measured".
        EvidenceRecord record = new EvidenceRecord(userId, type, type.trust());
        apply(record, request);
        return EvidenceResponse.from(evidence.save(record));
    }

    @Transactional
    public EvidenceResponse update(UUID userId, UUID id, EvidenceRequest request) {
        EvidenceRecord record = evidence.findByIdAndUserId(id, userId).orElseThrow(() -> CareerException.notFound("Evidence"));
        if (!record.type().wire().equals(request.type())) {
            throw CareerException.invalid("The type of existing evidence cannot be changed.");
        }
        apply(record, request);
        return EvidenceResponse.from(evidence.save(record));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        EvidenceRecord record = evidence.findByIdAndUserId(id, userId).orElseThrow(() -> CareerException.notFound("Evidence"));
        evidence.delete(record);
    }

    private void apply(EvidenceRecord record, EvidenceRequest request) {
        Set<String> seen = new HashSet<>();
        for (SkillRefBody ref : request.skills()) {
            if (!Catalog.SKILL_BY_ID.containsKey(ref.id())) {
                throw CareerException.unknownSkill(ref.id());
            }
            if (!seen.add(ref.id())) {
                throw CareerException.invalid("Skill listed twice: " + ref.id() + ".");
            }
        }
        record.apply(
                request.title().trim(),
                blankToNull(request.org()),
                request.date(),
                request.months(),
                request.score(),
                blankToNull(request.note()),
                request.skills().stream().map(s -> new SkillRef(s.id(), s.weight())).toList());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
