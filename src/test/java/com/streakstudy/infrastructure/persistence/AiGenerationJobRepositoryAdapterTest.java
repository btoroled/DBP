package com.streakstudy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.streakstudy.domain.model.AiGenerationJob;
import com.streakstudy.domain.model.AiGenerationJobStatus;
import com.streakstudy.domain.model.Document;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.infrastructure.persistence.adapter.AiGenerationJobRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.DocumentRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({
    InstitutionRepositoryAdapter.class,
    DocumentRepositoryAdapter.class,
    AiGenerationJobRepositoryAdapter.class
})
class AiGenerationJobRepositoryAdapterTest {

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired DocumentRepositoryAdapter documents;
    @Autowired AiGenerationJobRepositoryAdapter jobs;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldPersistJobInPendingStateWhenCreating() {
        long docId = setupDocumentInTenant();

        AiGenerationJob saved = jobs.save(
            AiGenerationJob.create(docId, 7L, "anthropic", "claude-haiku"));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.status()).isEqualTo(AiGenerationJobStatus.PENDING);
        assertThat(saved.documentId()).isEqualTo(docId);
        assertThat(saved.deckId()).isEqualTo(7L);
    }

    @Test
    void shouldTransitionToCompletedWithTokensAndCostWhenJobFinishes() {
        long docId = setupDocumentInTenant();
        AiGenerationJob saved = jobs.save(
            AiGenerationJob.create(docId, 7L, "anthropic", "claude-haiku"));

        AiGenerationJob completed = jobs.save(saved.withCompleted(1000, 500));

        assertThat(completed.status()).isEqualTo(AiGenerationJobStatus.COMPLETED);
        assertThat(completed.totalInputTokens()).isEqualTo(1000);
        assertThat(completed.totalOutputTokens()).isEqualTo(500);
        // 1000 * 0.80/1M + 500 * 4.00/1M = 0.0008 + 0.002 = 0.0028
        assertThat(completed.estimatedCostUsd()).isCloseTo(0.0028, within(1e-6));
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void shouldFindJobByDocumentIdAndStatusWhenQueryingForDuplicate() {
        long docId = setupDocumentInTenant();
        AiGenerationJob pending = jobs.save(
            AiGenerationJob.create(docId, 7L, "anthropic", "claude-haiku"));
        jobs.save(pending.withCompleted(100, 50));

        Optional<AiGenerationJob> completed = jobs.findByDocumentIdAndStatus(
            docId, AiGenerationJobStatus.COMPLETED);
        Optional<AiGenerationJob> running = jobs.findByDocumentIdAndStatus(
            docId, AiGenerationJobStatus.RUNNING);

        assertThat(completed).isPresent();
        assertThat(completed.get().status()).isEqualTo(AiGenerationJobStatus.COMPLETED);
        assertThat(running).isEmpty();
    }

    @Test
    void shouldPersistFailureMessageWhenJobFails() {
        long docId = setupDocumentInTenant();
        AiGenerationJob saved = jobs.save(
            AiGenerationJob.create(docId, 7L, "anthropic", "claude-haiku"));

        AiGenerationJob failed = jobs.save(saved.withFailed("Anthropic 503"));

        assertThat(failed.status()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(failed.errorMessage()).isEqualTo("Anthropic 503");
    }

    private long setupDocumentInTenant() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);
        Document doc = documents.save(Document.newUpload(inst, 99L, "x.pdf", 10L, "hash-job"));
        return doc.id();
    }
}
