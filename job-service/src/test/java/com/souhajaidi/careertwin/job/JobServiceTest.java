package com.souhajaidi.careertwin.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.souhajaidi.careertwin.job.api.JobDtos.JobRequest;
import com.souhajaidi.careertwin.job.domain.Job;
import com.souhajaidi.careertwin.job.repository.JobRepository;
import com.souhajaidi.careertwin.job.service.CareerClient;
import com.souhajaidi.careertwin.job.service.JobException;
import com.souhajaidi.careertwin.job.service.JobService;
import com.souhajaidi.careertwin.engine.ScoringEngine;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository repository;

    @Mock
    private CareerClient career;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private JobService service() {
        return new JobService(repository, new ScoringEngine(Clock.systemUTC()), career, mapper);
    }

    private static final String POSTING =
            "Backend Engineer. 3+ years of Java and Spring Boot.\nPostgreSQL and Docker required.\nKafka is a plus.";

    @Test
    @DisplayName("saving a posting keeps the raw text and stores parsed requirements")
    void createStoresRawAndParsed() {
        when(repository.save(any(Job.class))).thenAnswer(call -> call.getArgument(0));
        UUID user = UUID.randomUUID();

        var body = service().create(user, new JobRequest("Backend Engineer", "Acme", "Tunis", "", POSTING));

        assertThat(body.raw()).isEqualTo(POSTING);
        assertThat(body.requirements().years()).isEqualTo(3);
        var ids = body.requirements().skills().stream().map(s -> s.id()).toList();
        assertThat(ids).contains("java", "spring", "sql", "docker", "kafka");
        var kafka = body.requirements().skills().stream().filter(s -> s.id().equals("kafka")).findFirst().orElseThrow();
        assertThat(kafka.required()).as("'is a plus' marks a preferred skill").isFalse();
    }

    @Test
    @DisplayName("another user's job reads as missing")
    void ownershipIsScoped() {
        UUID stranger = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, stranger)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(stranger, id)).isInstanceOf(JobException.class);
        assertThatThrownBy(() -> service().delete(stranger, id)).isInstanceOf(JobException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("an empty job list does not call career-service")
    void emptyListSkipsRemoteCall() {
        UUID user = UUID.randomUUID();
        when(repository.findByUserIdOrderByCreatedAtDesc(user)).thenReturn(List.of());

        assertThat(service().list(user, "token")).isEmpty();
        verify(career, never()).match(anyString(), anyMap());
    }

    @Test
    @DisplayName("when scoring is down the list still loads, with null scores")
    void listDegradesWhenCareerServiceIsDown() {
        UUID user = UUID.randomUUID();
        when(repository.findByUserIdOrderByCreatedAtDesc(user))
                .thenReturn(List.of(new Job(user, "Dev", "", "", "", POSTING, "{\"skills\":[],\"years\":null,\"degree\":null,\"others\":[],\"ambiguous\":[],\"seniority\":\"Mid-level\"}")));
        when(career.match(anyString(), anyMap())).thenReturn(Map.of());

        var rows = service().list(user, "token");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).score()).isNull();
    }

    @Test
    @DisplayName("a single match fails loudly, not with a made-up score, when scoring is down")
    void matchFailsWhenCareerServiceIsDown() {
        UUID user = UUID.randomUUID();
        Job job = new Job(user, "Dev", "", "", "", POSTING, "{\"skills\":[],\"years\":null,\"degree\":null,\"others\":[],\"ambiguous\":[],\"seniority\":\"Mid-level\"}");
        when(repository.findByIdAndUserId(job.getId(), user)).thenReturn(Optional.of(job));
        when(career.match(anyString(), anyMap())).thenReturn(Map.<String, JsonNode>of());

        assertThatThrownBy(() -> service().match(user, job.getId(), "token"))
                .isInstanceOf(JobException.class)
                .hasMessageContaining("unavailable");
    }
}
