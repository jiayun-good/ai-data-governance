package com.mp.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "知识文档列表项")
public class KnowledgeDocVO {

    @Schema(description = "文档唯一标识")
    private String docId;

    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "来源文件名")
    private String source;

    @Schema(description = "切片数量")
    private Integer chunkCount;
}
