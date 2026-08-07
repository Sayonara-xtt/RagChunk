package com.xtsh.ragchunk.service.document;

import com.xtsh.ragchunk.dto.document.DocumentRecord;

import java.util.List;
import java.util.Optional;

public interface DocumentStore {

    DocumentRecord save(DocumentRecord doc);

    Optional<DocumentRecord> findById(String id);

    List<DocumentRecord> findByKbId(String kbId);
}
