package library.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("borrow_record")
@Schema(description = "借阅记录实体")
public class BorrowRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "图书ID")
    private Long bookId;
    @Schema(description = "副本ID")
    private Long copyId;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "借阅时间")
    private LocalDateTime borrowTime;
    @Schema(description = "应还时间")
    private LocalDateTime dueTime;
    @Schema(description = "实际归还时间")
    private LocalDateTime returnTime;
    @Schema(description = "状态（0:借阅中, 1:已归还, 2:已逾期）")
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}