package com.xiaoyan.aiassistant.document;

import com.xiaoyan.aiassistant.retrieval.Bm25Service;
import com.xiaoyan.aiassistant.retrieval.VectorNamespace;
import com.xiaoyan.aiassistant.retrieval.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 文档异步入库服务：解析、分块、向量化在后台线程执行，不阻塞上传请求。
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestService {
    private final DocumentMapper documentMapper;
    private final DocumentTextExtractor extractor;
    private final TextCleaner cleaner;
    private final DocumentChunkingService chunkingService;
    private final VectorStoreService vectorStoreService;
    private final Bm25Service bm25Service;

    // 后台执行解析入库，成功置 READY，失败置 FAILED 并记录原因。
    @Async("documentIngestExecutor")
    public void ingest(KbDocument document, byte[] bytes) {
        try {
            String text = cleaner.clean(extractor.extract(bytes));
            List<DocumentChunk> chunks = chunkingService.split(text);
            for (DocumentChunk chunk : chunks) {
                KbChunk kbChunk = toKbChunk(document, chunk);
                documentMapper.insertChunk(kbChunk);
                vectorStoreService.add(VectorNamespace.KNOWLEDGE, kbChunk.getVectorId(), kbChunk.getContent(), metadata(document, kbChunk));
            }
            document.setStatus(DocumentStatus.READY.name());
            document.setChunkCount(chunks.size());
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateDocument(document);
            bm25Service.rebuild(documentMapper.findAllChunks());
        } catch (Exception ex) {
            log.warn("Async document ingest failed for {}: {}", document.getFileName(), ex.getMessage());
            document.setStatus(DocumentStatus.FAILED.name());
            document.setErrorMessage(ex.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateDocument(document);
        }
    }

    // 将分块结果转换为数据库 chunk 实体。
    private KbChunk toKbChunk(KbDocument document, DocumentChunk chunk) {
        return new KbChunk(
                null,
                document.getId(),
                "doc-" + document.getId() + "-chunk-" + chunk.index() + "-" + UUID.randomUUID(),
                chunk.index(),
                chunk.title(),
                chunk.sectionPath(),
                chunk.content(),
                chunk.tokenEstimate(),
                LocalDateTime.now()
        );
    }

    // 构造写入向量库的元数据，数字统一转字符串以兼容 Pinecone。
    private Map<String, String> metadata(KbDocument document, KbChunk chunk) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("documentId", String.valueOf(document.getId()));
        metadata.put("chunkId", String.valueOf(chunk.getId()));
        metadata.put("title", nullToEmpty(chunk.getTitle()));
        metadata.put("source", nullToEmpty(document.getFileName()));
        metadata.put("chunkIndex", String.valueOf(chunk.getChunkIndex()));
        return metadata;
    }

    // 将空值转为空字符串。
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
