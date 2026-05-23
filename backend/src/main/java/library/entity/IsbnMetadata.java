package library.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("isbn_metadata")
@Schema(description = "ISBN书籍元数据")
public class IsbnMetadata {
    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "ISBN编号")
    private String isbn;
    @Schema(description = "书名")
    private String title;
    @Schema(description = "作者")
    private String author;
    @Schema(description = "出版社")
    private String publisher;
    @Schema(description = "出版日期")
    private String publishDate;
    @Schema(description = "封面图片URL")
    private String coverImage;
    @Schema(description = "数据来源")
    private String source;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
