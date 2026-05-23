package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新图书副本状态请求")
public class CopyStatusRequest {
    @Schema(description = "副本状态：0在馆可借，2丢失", allowableValues = {"0", "2"})
    private Integer status;
}
