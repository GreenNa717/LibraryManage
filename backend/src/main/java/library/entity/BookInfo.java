package library.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("book_info")
@Schema(description = "图书信息实体")
public class BookInfo {
    @TableId(type = IdType.AUTO)
    @Schema(description = "图书ID")
    private Long id;
    @Schema(description = "ISBN编号", example = "978-7-111-54742-5")
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
    @Schema(description = "总库存")
    private Integer totalStock;
    @Schema(description = "当前可借库存")
    private Integer currentStock;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}