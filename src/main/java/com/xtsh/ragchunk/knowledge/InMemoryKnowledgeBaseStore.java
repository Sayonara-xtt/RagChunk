package com.xtsh.ragchunk.knowledge;

import com.xtsh.ragchunk.knowledge.model.KnowledgeBase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "inmemory")
public class InMemoryKnowledgeBaseStore implements KnowledgeBaseStore {

    private final ConcurrentHashMap<String, KnowledgeBase> store = new ConcurrentHashMap<>();

    @Override
    public KnowledgeBase save(KnowledgeBase kb) {
        store.put(kb.getId(), kb);
        return kb;
    }

    @Override
    public Optional<KnowledgeBase> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<KnowledgeBase> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(KnowledgeBase::getCreatedAt).reversed())
                .toList();
    }
}
