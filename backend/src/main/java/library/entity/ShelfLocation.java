package library.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("shelf_location")
@Schema(description = "库位实体")
public class ShelfLocation {
    @TableId(type = IdType.AUTO)
    @Schema(description = "库位ID")
    private Long id;
    @Schema(description = "库位名称")
    private String locationName;
    @Schema(description = "关联分类ID")
    private Long categoryId;
    @Schema(description = "最大容量")
    private Integer maxCapacity;
    @Schema(description = "当前存放数量")
    private Integer currentCount;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
