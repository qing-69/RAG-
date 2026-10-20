package com.xiaoyan.aiassistant.document;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

// 文档正文提取器。
@Component
public class DocumentTextExtractor {
    private final ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

    // 使用 Apache Tika 从上传文件中提取正文。
    public String extract(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            Document document = parser.parse(inputStream);
            return document.text();
        }
    }

    // 从字节数组提取正文，供异步入库线程使用。
    public String extract(byte[] bytes) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            Document document = parser.parse(inputStream);
            return document.text();
        }
    }
}
