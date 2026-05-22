package com.xtsh.ragchunk.document;

import com.xtsh.ragchunk.document.model.DocumentRecord;

import java.util.List;
import java.util.Optional;

public interface DocumentStore {

    DocumentRecord save(DocumentRecord doc);

    Optional<DocumentRecord> findById(String id);

    List<DocumentRecord> findByKbId(String kbId);
}
