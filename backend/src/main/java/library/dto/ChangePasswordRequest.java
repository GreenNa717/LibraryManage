package library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前管理员密码修改请求")
public class ChangePasswordRequest {
    @Schema(description = "原密码")
    private String oldPassword;
    @Schema(description = "新密码")
    private String newPassword;
}
