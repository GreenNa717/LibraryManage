package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "归还操作结果视图")
public class ReturnActionResult {
    @Schema(description = "借阅记录ID")
    private Long recordId;
    @Schema(description = "书名")
    private String bookTitle;
    @Schema(description = "副本编号")
    private String copyCode;
    @Schema(description = "归还时间")
    private LocalDateTime returnTime;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "提示信息")
    private String message;
}
