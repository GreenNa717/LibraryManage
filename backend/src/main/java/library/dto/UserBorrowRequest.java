package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "普通用户借阅请求")
public class UserBorrowRequest {
    @Schema(description = "图书副本ID（与copyCode二选一）")
    private Long copyId;
    @Schema(description = "图书副本编码（扫码内容）")
    private String copyCode;
}
