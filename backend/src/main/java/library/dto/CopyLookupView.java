package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "副本扫码查询视图")
public class CopyLookupView {
    @Schema(description = "副本ID")
    private Long copyId;
    @Schema(description = "副本编号")
    private String copyCode;
    @Schema(description = "副本状态（0:在馆可借, 1:已借出, 2:丢失）")
    private Integer copyStatus;
    @Schema(description = "副本状态文案")
    private String copyStatusText;
    @Schema(description = "图书ID")
    private Long bookId;
    @Schema(description = "书名")
    private String title;
    @Schema(description = "作者")
    private String author;
    @Schema(description = "ISBN")
    private String isbn;
    @Schema(description = "封面图片URL")
    private String coverImage;
    @Schema(description = "图书当前可借库存")
    private Integer currentStock;
    @Schema(description = "是否可借阅")
    private Boolean borrowable;
    @Schema(description = "是否可归还")
    private Boolean returnable;
    @Schema(description = "提示信息")
    private String message;
}
