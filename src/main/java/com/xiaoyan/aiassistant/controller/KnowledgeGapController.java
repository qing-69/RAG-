package com.xiaoyan.aiassistant.controller;

import com.xiaoyan.aiassistant.gap.KnowledgeGapCluster;
import com.xiaoyan.aiassistant.gap.KnowledgeGapService;
import com.xiaoyan.aiassistant.gap.KnowledgeGapSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 知识缺口分析接口。
@RestController
@RequestMapping("/api/knowledge-gaps")
@RequiredArgsConstructor
public class KnowledgeGapController {
    private final KnowledgeGapService knowledgeGapService;

    // 查询时间窗内的提问规模和低置信占比。
    @GetMapping("/summary")
    public KnowledgeGapSummary summary(@RequestParam(required = false) Integer days) {
        return knowledgeGapService.summarize(days == null ? 0 : days);
    }

    // 查询规则聚类后的缺口主题列表。
    @GetMapping("/clusters")
    public List<KnowledgeGapCluster> clusters(@RequestParam(required = false) Integer days) {
        return knowledgeGapService.cluster(days == null ? 0 : days);
    }
}
