package library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.dto.LoginRequest;
import library.dto.LoginResponse;
import library.common.RoleConstants;
import library.service.AuthService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "通用登录认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "通用登录", description = "用户名密码登录，返回JWT Token。小程序端允许普通用户和管理员登录，游客不可登录。")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getUsername(), request.getPassword());
        if (RoleConstants.isGuest(response.getRole())) {
            return Result.error("游客账号暂无借阅权限");
        }
        return Result.ok(response);
    }
}
