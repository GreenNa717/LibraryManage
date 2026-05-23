package library.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("book_category")
@Schema(description = "图书分类实体")
public class BookCategory {
    @TableId(type = IdType.AUTO)
    @Schema(description = "分类ID")
    private Long id;
    @Schema(description = "分类名称", example = "计算机")
    private String categoryName;
    @Schema(description = "排序值（越大越靠前）")
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}