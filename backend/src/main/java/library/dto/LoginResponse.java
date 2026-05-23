package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "登录响应数据")
@Data
public class LoginResponse {
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "角色（0:管理员, 1:读者, 2:协管员, 3:游客）")
    private Integer role;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "JWT Token")
    private String token;

    public LoginResponse(Long userId, String username, String realName, Integer role, String token) {
        this(userId, username, realName, role, null, null, token);
    }

    public LoginResponse(Long userId, String username, String realName, Integer role, String nickname, String avatar, String token) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.nickname = nickname;
        this.avatar = avatar;
        this.token = token;
    }
}