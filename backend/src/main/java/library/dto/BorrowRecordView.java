package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "借阅记录展示视图")
public class BorrowRecordView {
    @Schema(description = "借阅记录ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "读者真实姓名")
    private String realName;
    @Schema(description = "读者昵称")
    private String nickname;
    @Schema(description = "读者手机号")
    private String phone;
    @Schema(description = "图书ID")
    private Long bookId;
    @Schema(description = "书名")
    private String bookTitle;
    @Schema(description = "作者")
    private String bookAuthor;
    @Schema(description = "ISBN")
    private String isbn;
    @Schema(description = "封面图片URL")
    private String coverImage;
    @Schema(description = "副本ID")
    private Long copyId;
    @Schema(description = "副本编号")
    private String copyCode;
    @Schema(description = "借阅时间")
    private LocalDateTime borrowTime;
    @Schema(description = "应还时间")
    private LocalDateTime dueTime;
    @Schema(description = "实际归还时间")
    private LocalDateTime returnTime;
    @Schema(description = "状态（0:借阅中, 1:已归还, 2:已逾期）")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
