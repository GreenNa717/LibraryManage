package library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.common.SecurityUtils;
import library.dto.UpdateProfileRequest;
import library.entity.SysUser;
import library.service.SysUserService;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@Tag(name = "账户中心", description = "所有登录用户可用的资料接口")
@RestController
@RequestMapping("/api/account/me")
public class AccountController {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final SysUserService sysUserService;

    public AccountController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Operation(summary = "获取当前登录用户资料")
    @GetMapping
    public Result<SysUser> me() {
        SysUser user = sysUserService.getById(SecurityUtils.currentUserId());
        if (user == null) throw new RuntimeException("用户不存在");
        return Result.ok(maskSensitive(user));
    }

    @Operation(summary = "更新当前登录用户资料", description = "支持更新姓名、昵称、手机号、邮箱、头像")
    @PutMapping("/profile")
    public Result<SysUser> updateProfile(@RequestBody UpdateProfileRequest request) {
        if (request == null) throw new RuntimeException("请求参数不能为空");
        SysUser user = sysUserService.getById(SecurityUtils.currentUserId());
        if (user == null) throw new RuntimeException("用户不存在");
        applyProfilePatch(user, request);
        sysUserService.updateById(user);
        SysUser latest = sysUserService.getById(user.getId());
        return Result.ok(maskSensitive(latest));
    }

    private void applyProfilePatch(SysUser user, UpdateProfileRequest request) {
        String realName = trim(request.getRealName());
        String nickname = trim(request.getNickname());
        String phone = trim(request.getPhone());
        String email = trim(request.getEmail());
        String avatar = trim(request.getAvatar());

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

    private SysUser maskSensitive(SysUser user) {
        user.setPassword(null);
        return user;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
