package library.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@Schema(description = "用户实体")
public class SysUser {
    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码（BCrypt加密）", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "头像（URL或Base64）")
    private String avatar;
    @Schema(description = "角色（0:超级管理员, 1:普通用户, 2:协管员, 3:游客）")
    private Integer role;
    @Schema(description = "状态（1:正常, 0:封禁）")
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
