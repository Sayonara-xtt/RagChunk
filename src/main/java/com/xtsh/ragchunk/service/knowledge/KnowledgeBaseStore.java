package com.xtsh.ragchunk.service.knowledge;

import com.xtsh.ragchunk.dto.knowledge.KnowledgeBase;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseStore {

    KnowledgeBase save(KnowledgeBase kb);

    Optional<KnowledgeBase> findById(String id);

    List<KnowledgeBase> findAll();
}
