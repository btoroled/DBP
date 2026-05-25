package com.streakstudy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.streakstudy.domain.model.Document;
import com.streakstudy.domain.model.DocumentStatus;
import com.streakstudy.domain.model.Institution;
import com.streakstudy.infrastructure.persistence.adapter.DocumentRepositoryAdapter;
import com.streakstudy.infrastructure.persistence.adapter.InstitutionRepositoryAdapter;
import com.streakstudy.infrastructure.tenancy.TenantContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import({InstitutionRepositoryAdapter.class, DocumentRepositoryAdapter.class})
class DocumentRepositoryAdapterTest {

    @Autowired InstitutionRepositoryAdapter institutions;
    @Autowired DocumentRepositoryAdapter documents;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldSaveAndAssignIdAndCreatedAtWhenUploadingDocument() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);

        Document saved = documents.save(Document.newUpload(inst, 99L, "apuntes.pdf", 1024L, "hash-a"));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(saved.uploadedBy()).isEqualTo(99L);
    }

    @Test
    void shouldFindDocumentByIdAfterSaving() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);
        Document saved = documents.save(Document.newUpload(inst, 99L, "x.pdf", 10L, "h1"));

        Optional<Document> found = documents.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.get().fileHash()).isEqualTo("h1");
    }

    @Test
    void shouldFindDocumentByFileHashForDeduplication() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);
        documents.save(Document.newUpload(inst, 99L, "first.pdf", 10L, "unique-hash"));

        Optional<Document> dup = documents.findByFileHash("unique-hash");
        Optional<Document> missing = documents.findByFileHash("does-not-exist");

        assertThat(dup).isPresent();
        assertThat(dup.get().originalFilename()).isEqualTo("first.pdf");
        assertThat(missing).isEmpty();
    }

    @Test
    void shouldPersistMarkdownAndStatusTransitionsWhenProcessing() {
        Long inst = institutions.save(Institution.newInstance("UTEC", "utec")).id();
        TenantContext.set(inst);
        Document doc = documents.save(Document.newUpload(inst, 99L, "x.pdf", 10L, "h2"));

        // PROCESSING
        documents.save(doc.withStatus(DocumentStatus.PROCESSING));
        Document after = documents.findById(doc.id()).orElseThrow();
        assertThat(after.status()).isEqualTo(DocumentStatus.PROCESSING);

        // READY con markdown
        documents.save(after.withMarkdown("# titulo"));
        Document ready = documents.findById(doc.id()).orElseThrow();
        assertThat(ready.status()).isEqualTo(DocumentStatus.READY);
        assertThat(ready.markdownContent()).isEqualTo("# titulo");
    }
}
