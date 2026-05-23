package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新增图书副本请求")
public class CopyBatchRequest {
    @Schema(description = "新增副本数量", example = "1")
    private Integer quantity;
    @Schema(description = "库位ID，不传则使用图书默认库位")
    private Long locationId;
}
