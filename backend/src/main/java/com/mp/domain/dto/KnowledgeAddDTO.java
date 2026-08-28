package com.mp.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "添加知识文档请求参数")
public class KnowledgeAddDTO {

    @Schema(description = "文档标题", example = "数据质量最佳实践")
    private String title;

    @Schema(description = "文档全文内容")
    private String content;

    @Schema(description = "来源文件名（可选）", example = "best_practice.md")
    private String source;
}
