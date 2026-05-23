package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新增用户请求")
public class CreateUserRequest {
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    @Schema(description = "初始密码，至少6位", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "头像URL或Base64")
    private String avatar;
    @Schema(description = "角色：1普通用户，2协管员，3游客")
    private Integer role;
    @Schema(description = "状态：1正常，0封禁")
    private Integer status;
}
