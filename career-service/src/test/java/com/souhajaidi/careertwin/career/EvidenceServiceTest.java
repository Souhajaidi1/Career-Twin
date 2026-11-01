package com.souhajaidi.careertwin.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.souhajaidi.careertwin.career.api.dto.Dtos.EvidenceRequest;
import com.souhajaidi.careertwin.career.api.dto.Dtos.SkillRefBody;
import com.souhajaidi.careertwin.career.domain.EvidenceRecord;
import com.souhajaidi.careertwin.career.repository.EvidenceRepository;
import com.souhajaidi.careertwin.career.service.CareerException;
import com.souhajaidi.careertwin.career.service.EvidenceService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    private EvidenceRepository repository;

    private EvidenceService service() {
        return new EvidenceService(repository);
    }

    private static EvidenceRequest request(String type, String... skills) {
        return new EvidenceRequest(
                type,
                "Something",
                null,
                null,
                3.0,
                null,
                null,
                java.util.Arrays.stream(skills).map(s -> new SkillRefBody(s, 1.0)).toList());
    }

    @Test
    @DisplayName("measured evidence types cannot be entered by hand")
    void rejectsReservedTypes() {
        UUID user = UUID.randomUUID();
        assertThatThrownBy(() -> service().create(user, request("assessment", "java")))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("in-product");
        assertThatThrownBy(() -> service().create(user, request("interview", "java")))
                .isInstanceOf(CareerException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("unknown skills are rejected before anything is stored")
    void rejectsUnknownSkill() {
        assertThatThrownBy(() -> service().create(UUID.randomUUID(), request("project", "not-a-skill")))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("not-a-skill");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("the same skill listed twice is rejected")
    void rejectsDuplicateSkill() {
        assertThatThrownBy(() -> service().create(UUID.randomUUID(), request("project", "java", "java")))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("twice");
    }

    @Test
    @DisplayName("origin comes from the evidence type, not from the caller")
    void originIsDerived() {
        when(repository.save(any(EvidenceRecord.class))).thenAnswer(call -> call.getArgument(0));
        var claim = service().create(UUID.randomUUID(), request("claim", "java"));
        var project = service().create(UUID.randomUUID(), request("project", "java"));
        assertThat(claim.origin()).isEqualTo("claimed");
        assertThat(project.origin()).isEqualTo("stated");
    }

    @Test
    @DisplayName("updating or deleting someone else's evidence looks like it does not exist")
    void ownershipIsScopedByQuery() {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, stranger)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(stranger, id, request("project", "java")))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("not found");
        assertThatThrownBy(() -> service().delete(stranger, id)).isInstanceOf(CareerException.class);
        verify(repository, never()).delete(any());
        assertThat(owner).isNotEqualTo(stranger);
    }

    @Test
    @DisplayName("only hand-enterable types are creatable")
    void creatableTypes() {
        assertThat(EvidenceService.userCreatable(com.souhajaidi.careertwin.engine.EvidenceType.PROJECT)).isTrue();
        assertThat(EvidenceService.userCreatable(com.souhajaidi.careertwin.engine.EvidenceType.ASSESSMENT)).isFalse();
        assertThat(List.of(com.souhajaidi.careertwin.engine.EvidenceType.values())).hasSize(8);
    }
}
