package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "移动图书副本库位请求")
public class CopyLocationRequest {
    @Schema(description = "目标库位ID，传空表示清除库位")
    private Long locationId;
}
