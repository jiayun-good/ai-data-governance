package com.mp.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新知识文档请求参数")
public class KnowledgeUpdateDTO {

    @Schema(description = "文档标题", example = "数据质量最佳实践")
    private String title;

    @Schema(description = "文档全文内容")
    private String content;
}
