package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前管理员资料更新请求")
public class UpdateProfileRequest {
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
}
