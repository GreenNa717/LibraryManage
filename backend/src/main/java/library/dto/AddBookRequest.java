package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新增图书请求")
public class AddBookRequest {
    @Schema(description = "ISBN编号")
    private String isbn;
    @Schema(description = "书名")
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
    @Schema(description = "入库数量")
    private Integer quantity;
}
