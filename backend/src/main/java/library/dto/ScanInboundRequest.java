package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码入库请求")
public class ScanInboundRequest {
    @Schema(description = "扫码内容（通常为ISBN）")
    private String scannedCode;
    @Schema(description = "ISBN编号，空则回退使用scannedCode")
    private String isbn;
    @Schema(description = "书名（新书必填）")
    private String title;
    @Schema(description = "作者")
    private String author;
    @Schema(description = "分类ID")
    private Long categoryId;
    @Schema(description = "库位ID")
    private Long locationId;
    @Schema(description = "封面图片URL")
    private String coverImage;
    @Schema(description = "图书简介")
    private String description;
    @Schema(description = "入库数量，默认1")
    private Integer quantity;
}
