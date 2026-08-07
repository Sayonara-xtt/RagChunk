package com.xtsh.ragchunk.service.document;

import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.dto.document.UploadBatchRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "inmemory")
public class InMemoryUploadBatchStore implements UploadBatchStore {

    private final ConcurrentHashMap<String, UploadBatchRecord> store = new ConcurrentHashMap<>();

    @Override
    public UploadBatchRecord save(UploadBatchRecord batch) {
        store.put(batch.getId(), batch);
        return batch;
    }

    @Override
    public Optional<UploadBatchRecord> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<UploadBatchRecord> findByKbId(String kbId, int limit) {
        return store.values().stream()
                .filter(b -> kbId.equals(b.getKbId()))
                .sorted(Comparator.comparing(UploadBatchRecord::getCreatedAt).reversed())
                .limit(limit)
                .toList();
    }
}
