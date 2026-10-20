package com.xiaoyan.aiassistant.document;

import com.xiaoyan.aiassistant.config.AppProperties;
import com.xiaoyan.aiassistant.retrieval.VectorNamespace;
import com.xiaoyan.aiassistant.retrieval.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 内存向量库启动预热：应用重启后从数据库重建全部向量，解决内存库清空导致 vectorScore=0 的问题。
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexWarmupService {

    private final DocumentMapper documentMapper;
    private final VectorStoreService vectorStoreService;
    private final AppProperties properties;

    // 应用就绪后异步重建向量索引，不阻塞启动。
    @Async("documentIngestExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        if (isPersistentStore()) {
            log.info("使用持久化向量库（Pinecone），跳过启动预热。");
            return;
        }
        try {
            List<KbChunk> chunks = documentMapper.findAllChunks();
            if (chunks.isEmpty()) {
                log.info("知识库无分块数据，跳过向量预热。");
                return;
            }
            log.info("开始向量预热，共 {} 个分块…", chunks.size());
            Map<Long, String> documentNames = loadDocumentNames();
            int rebuilt = 0;
            for (KbChunk chunk : chunks) {
                try {
                    vectorStoreService.add(
                            VectorNamespace.KNOWLEDGE,
                            chunk.getVectorId(),
                            chunk.getContent(),
                            metadata(chunk, documentNames));
                    rebuilt++;
                } catch (Exception ex) {
                    log.warn("分块向量重建失败 chunkId={}: {}", chunk.getId(), ex.getMessage());
                }
            }
            log.info("向量预热完成，成功重建 {}/{} 个分块。", rebuilt, chunks.size());
        } catch (Exception ex) {
            log.warn("向量预热失败，不影响启动，后续检索可能缺少向量结果: {}", ex.getMessage());
        }
    }

    // 判断是否使用持久化向量库，是则无需预热。
    private boolean isPersistentStore() {
        AppProperties.Pinecone pinecone = properties.getPinecone();
        return pinecone.isEnabled() && StringUtils.hasText(pinecone.getApiKey());
    }

    // 加载文档 id → 文件名映射，用于向量元数据的 source 字段。
    private Map<Long, String> loadDocumentNames() {
        Map<Long, String> names = new HashMap<>();
        try {
            for (KbDocument doc : documentMapper.findDocuments()) {
                names.put(doc.getId(), doc.getFileName());
            }
        } catch (Exception ex) {
            log.warn("加载文档名称失败，source 字段将为空: {}", ex.getMessage());
        }
        return names;
    }

    // 构造向量元数据，与 DocumentIngestService.metadata() 保持一致。
    private Map<String, String> metadata(KbChunk chunk, Map<Long, String> documentNames) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("documentId", String.valueOf(chunk.getDocumentId()));
        metadata.put("chunkId", String.valueOf(chunk.getId()));
        metadata.put("title", nullToEmpty(chunk.getTitle()));
        metadata.put("source", nullToEmpty(documentNames.get(chunk.getDocumentId())));
        metadata.put("chunkIndex", String.valueOf(chunk.getChunkIndex()));
        return metadata;
    }

    // 将空值转为空字符串。
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
