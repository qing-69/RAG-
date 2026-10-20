package com.xiaoyan.aiassistant.document;

import com.xiaoyan.aiassistant.retrieval.Bm25Service;
import com.xiaoyan.aiassistant.retrieval.VectorNamespace;
import com.xiaoyan.aiassistant.retrieval.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// 文档入库、列表和删除的业务服务。
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentMapper documentMapper;
    private final DocumentIngestService documentIngestService;
    private final VectorStoreService vectorStoreService;
    private final Bm25Service bm25Service;

    // 上传入口：先落库占位，再异步解析入库，避免大文档阻塞 HTTP 请求；不加事务，保证占位行先于异步任务提交。
    public DocumentUploadResponse upload(MultipartFile file) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        KbDocument document = new KbDocument(
                null,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                DocumentStatus.PROCESSING.name(),
                0,
                null,
                now,
                now
        );
        documentMapper.insertDocument(document);
        // 文件字节在请求线程内读出，异步线程不能再访问 MultipartFile。
        byte[] bytes = file.getBytes();
        documentIngestService.ingest(document, bytes);
        return new DocumentUploadResponse(document.getId(), document.getFileName(), document.getStatus(), 0);
    }

    // 查询文档元数据列表。
    public List<KbDocument> list() {
        return documentMapper.findDocuments();
    }

    // 删除文档及其 chunk、向量和 BM25 索引。
    @Transactional
    public void delete(Long documentId) {
        List<KbChunk> chunks = documentMapper.findChunksByDocumentId(documentId);
        vectorStoreService.remove(VectorNamespace.KNOWLEDGE, chunks.stream().map(KbChunk::getVectorId).toList());
        documentMapper.deleteChunksByDocumentId(documentId);
        documentMapper.deleteDocument(documentId);
        bm25Service.rebuild(documentMapper.findAllChunks());
    }
}
