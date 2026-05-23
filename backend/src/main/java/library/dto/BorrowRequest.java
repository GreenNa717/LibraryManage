package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "借阅请求")
public class BorrowRequest {
    @Schema(description = "读者用户ID")
    private Long userId;
    @Schema(description = "副本ID")
    private Long copyId;
}
