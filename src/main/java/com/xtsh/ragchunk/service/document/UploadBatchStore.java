package com.xtsh.ragchunk.service.document;

import com.xtsh.ragchunk.dto.document.UploadBatchRecord;

import java.util.List;
import java.util.Optional;

public interface UploadBatchStore {

    UploadBatchRecord save(UploadBatchRecord batch);

    Optional<UploadBatchRecord> findById(String id);

    List<UploadBatchRecord> findByKbId(String kbId, int limit);
}
