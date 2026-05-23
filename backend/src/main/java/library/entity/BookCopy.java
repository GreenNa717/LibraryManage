package library.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("book_copy")
@Schema(description = "图书副本实体")
public class BookCopy {
    @TableId(type = IdType.AUTO)
    @Schema(description = "副本ID")
    private Long id;
    @Schema(description = "关联图书ID")
    private Long bookInfoId;
    @Schema(description = "副本编号", example = "978-7-111-54742-5-001")
    private String copyCode;
    @Schema(description = "状态（0:在馆可借, 1:已借出, 2:丢失）")
    private Integer status;
    @Schema(description = "库位ID")
    private Long locationId;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
