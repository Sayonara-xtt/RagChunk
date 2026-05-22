package com.xtsh.ragchunk.knowledge;

import com.xtsh.ragchunk.knowledge.model.KnowledgeBase;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseStore {

    KnowledgeBase save(KnowledgeBase kb);

    Optional<KnowledgeBase> findById(String id);

    List<KnowledgeBase> findAll();
}
