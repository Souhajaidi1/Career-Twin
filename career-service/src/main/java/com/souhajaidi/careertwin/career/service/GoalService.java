package com.souhajaidi.careertwin.career.service;

import com.souhajaidi.careertwin.career.api.dto.Dtos.GoalRequest;
import com.souhajaidi.careertwin.career.api.dto.Dtos.GoalResponse;
import com.souhajaidi.careertwin.career.domain.Goal;
import com.souhajaidi.careertwin.career.repository.GoalRepository;
import com.souhajaidi.careertwin.engine.Catalog;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {

    private final GoalRepository goals;

    public GoalService(GoalRepository goals) {
        this.goals = goals;
    }

    /** Replaces the user's active goal. The partial unique index guarantees there is only ever one. */
    @Transactional
    public GoalResponse set(UUID userId, GoalRequest request) {
        if (!Catalog.ROLE_BY_ID.containsKey(request.roleId())) {
            throw CareerException.unknownRole(request.roleId());
        }
        if (!Catalog.SENIORITIES.contains(request.seniority())) {
            throw CareerException.unknownSeniority(request.seniority());
        }
        goals.findByUserIdAndActiveTrue(userId).forEach(Goal::deactivate);
        goals.flush();
        return GoalResponse.from(goals.save(new Goal(userId, request.roleId(), request.seniority())));
    }
}
