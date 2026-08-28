package com.mp.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "知识检索结果片段")
public class KnowledgeSearchResultVO {

    @Schema(description = "片段内容")
    private String content;

    @Schema(description = "来源文件名")
    private String source;

    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "切片索引")
    private Integer chunkIndex;

    @Schema(description = "相似度分数")
    private Double score;
}
