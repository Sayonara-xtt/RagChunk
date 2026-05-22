package com.xtsh.ragchunk.document;

import com.xtsh.ragchunk.document.model.DocumentRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "inmemory")
public class InMemoryDocumentStore implements DocumentStore {

    private final ConcurrentHashMap<String, DocumentRecord> store = new ConcurrentHashMap<>();

    @Override
    public DocumentRecord save(DocumentRecord doc) {
        store.put(doc.getId(), doc);
        return doc;
    }

    @Override
    public Optional<DocumentRecord> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<DocumentRecord> findByKbId(String kbId) {
        return store.values().stream()
                .filter(d -> kbId.equals(d.getKbId()))
                .collect(Collectors.toList());
    }
}
