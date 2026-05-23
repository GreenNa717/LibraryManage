package library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.JwtUtil;
import library.common.Result;
import library.dto.ChangePasswordRequest;
import library.dto.LoginRequest;
import library.dto.LoginResponse;
import library.dto.UpdateProfileRequest;
import library.entity.SysUser;
import library.service.AuthService;
import library.service.SysUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@Tag(name = "认证管理", description = "登录认证相关接口")
@RestController
@RequestMapping("/api/admin")
public class AuthController {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AuthService authService;
    private final SysUserService sysUserService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, SysUserService sysUserService, BCryptPasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "管理员登录", description = "用户名密码登录，返回JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getUsername(), request.getPassword());
        return Result.ok(response);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<SysUser> me() {
        SysUser user = requireCurrentUser();
        return Result.ok(maskSensitive(user));
    }

    @Operation(summary = "更新当前管理员资料", description = "可更新真实姓名、昵称、手机号、邮箱和头像")
    @PutMapping("/me/profile")
    public Result<SysUser> updateProfile(@RequestBody UpdateProfileRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }

        SysUser user = requireCurrentUser();
        applyProfilePatch(user, request, true);
        sysUserService.updateById(user);

        SysUser latest = sysUserService.getById(user.getId());
        return Result.ok(maskSensitive(latest));
    }

    @Operation(summary = "修改当前管理员密码")
    @PutMapping("/me/password")
    public Result<Void> changeMyPassword(@RequestBody ChangePasswordRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        String oldPassword = request.getOldPassword() == null ? "" : request.getOldPassword();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword();
        if (oldPassword.isBlank() || newPassword.isBlank()) {
            throw new RuntimeException("原密码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }
        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("新密码不能与原密码相同");
        }

        SysUser user = requireCurrentUser();
        if (!authService.passwordMatches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserService.updateById(user);
        return Result.ok();
    }

    private SysUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("登录状态已失效，请重新登录");
        }
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private SysUser maskSensitive(SysUser user) {
        user.setPassword(null);
        return user;
    }

    private void applyProfilePatch(SysUser user, UpdateProfileRequest request, boolean requireRealName) {
        String realName = trim(request.getRealName());
        String nickname = trim(request.getNickname());
        String phone = trim(request.getPhone());
        String email = trim(request.getEmail());
        String avatar = trim(request.getAvatar());

        if (requireRealName && (realName == null || realName.isEmpty())) {
            throw new RuntimeException("姓名不能为空");
        }
        if (realName != null) {
            if (realName.isEmpty()) throw new RuntimeException("姓名不能为空");
            if (realName.length() > 50) throw new RuntimeException("姓名长度不能超过50个字符");
            user.setRealName(realName);
        }
        if (nickname != null) {
            if (!nickname.isEmpty() && nickname.length() > 50) {
                throw new RuntimeException("昵称长度不能超过50个字符");
            }
            user.setNickname(nickname.isEmpty() ? null : nickname);
        }
        if (phone != null) {
            if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                throw new RuntimeException("手机号格式不正确");
            }
            user.setPhone(phone);
        }
        if (email != null) {
            if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
                throw new RuntimeException("邮箱格式不正确");
            }
            user.setEmail(email.isEmpty() ? null : email);
        }
        if (avatar != null) {
            if (avatar.length() > 2_000_000) {
                throw new RuntimeException("头像数据过大，请压缩后重试");
            }
            user.setAvatar(avatar.isEmpty() ? null : avatar);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
