package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "借阅操作结果视图")
public class BorrowActionResult {
    @Schema(description = "借阅记录ID")
    private Long recordId;
    @Schema(description = "图书ID")
    private Long bookId;
    @Schema(description = "书名")
    private String bookTitle;
    @Schema(description = "副本ID")
    private Long copyId;
    @Schema(description = "副本编号")
    private String copyCode;
    @Schema(description = "借阅时间")
    private LocalDateTime borrowTime;
    @Schema(description = "应还时间")
    private LocalDateTime dueTime;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "提示信息")
    private String message;
}
