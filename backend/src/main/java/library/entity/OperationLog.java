package library.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
@Schema(description = "操作日志实体")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Schema(description = "操作人姓名")
    private String operatorName;
    @Schema(description = "操作类型")
    private String actionType;
    @Schema(description = "操作对象描述")
    private String targetDesc;
    @Schema(description = "操作详情")
    private String detail;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
