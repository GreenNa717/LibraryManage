package library.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.RoleConstants;
import library.common.Result;
import library.common.SecurityUtils;
import library.dto.CreateUserRequest;
import library.dto.UpdateProfileRequest;
import library.dto.UserDetailVO;
import library.entity.SysUser;
import library.service.BorrowRecordService;
import library.service.SysUserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@Tag(name = "用户管理", description = "读者账号管理接口")
@RestController
@RequestMapping("/api/admin/users")
public class UserController {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final SysUserService sysUserService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final BorrowRecordService borrowRecordService;

    public UserController(SysUserService sysUserService, BCryptPasswordEncoder passwordEncoder, BorrowRecordService borrowRecordService) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.borrowRecordService = borrowRecordService;
    }

    @Operation(summary = "分页查询用户列表", description = "支持按用户名、姓名、手机号模糊搜索")
    @GetMapping
    public Result<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<SysUser> page = new Page<>(current, size);
        page = sysUserService.getUserPage(page, keyword);
        page.getRecords().forEach(this::maskSensitive);
        return Result.ok(page);
    }

    @Operation(summary = "新增用户", description = "创建普通读者或游客账号，超级管理员可创建协管员")
    @PostMapping
    public Result<SysUser> create(@RequestBody CreateUserRequest request) {
        if (request == null) throw new RuntimeException("请求参数不能为空");
        String username = trim(request.getUsername());
        String password = trim(request.getPassword());
        if (username == null || username.isEmpty()) throw new RuntimeException("用户名不能为空");
        if (password == null || password.length() < 6) throw new RuntimeException("密码长度不能少于6位");
        if (sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        Integer role = request.getRole() == null ? RoleConstants.USER : request.getRole();
        if (!RoleConstants.isUser(role) && !RoleConstants.isGuest(role) && !RoleConstants.isAssistantAdmin(role)) {
            throw new RuntimeException("目标角色不合法");
        }
        if (RoleConstants.isAssistantAdmin(role)) {
            SecurityUtils.requireRole(RoleConstants.ROLE_SUPER_ADMIN, "只有超级管理员可创建协管员");
        }
        Integer status = request.getStatus() == null ? 1 : request.getStatus();
        if (status != 0 && status != 1) throw new RuntimeException("状态值非法");

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(status);
        applyProfilePatch(user, toProfileRequest(request));
        sysUserService.save(user);
        return Result.ok(maskSensitive(user));
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    public Result<SysUser> detail(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        return Result.ok(maskSensitive(user));
    }

    @Operation(summary = "查询用户详情（含借阅统计）")
    @GetMapping("/{id}/detail")
    public Result<UserDetailVO> userDetail(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) throw new RuntimeException("用户不存在");

        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime() != null ? user.getCreateTime().toString().replace("T", " ") : null);

        vo.setTotalBorrows(borrowRecordService.countByUserId(id));
        vo.setCurrentBorrows(borrowRecordService.countByUserIdAndStatus(id, 0));
        vo.setOverdueCount(borrowRecordService.countByUserIdAndStatus(id, 2));
        vo.setReturnedCount(borrowRecordService.countByUserIdAndStatus(id, 1));

        return Result.ok(vo);
    }

    @Operation(summary = "封禁/解封用户")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status != 0 && status != 1) {
            throw new RuntimeException("状态值非法");
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        ensureManageableTarget(user);
        user.setStatus(status);
        sysUserService.updateById(user);
        return Result.ok();
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("新密码不能为空");
        }
        if (password.length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        ensureManageableTarget(user);
        user.setPassword(passwordEncoder.encode(password));
        sysUserService.updateById(user);
        return Result.ok();
    }

    @Operation(summary = "设置用户角色", description = "仅超级管理员可将普通用户提升为协管员，或将协管员降级为普通用户")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam Integer role) {
        SecurityUtils.requireRole(RoleConstants.ROLE_SUPER_ADMIN, "只有超级管理员可设置角色");
        if (role == null || !RoleConstants.isValid(role) || RoleConstants.isSuperAdmin(role)) {
            throw new RuntimeException("目标角色不合法");
        }
        SysUser target = sysUserService.getById(id);
        if (target == null) throw new RuntimeException("用户不存在");
        if (RoleConstants.isSuperAdmin(target.getRole())) {
            throw new RuntimeException("超级管理员账号不可修改角色");
        }
        if (RoleConstants.isAssistantAdmin(role) && !RoleConstants.isUser(target.getRole())) {
            throw new RuntimeException("仅可将普通用户提升为协管员");
        }
        if (RoleConstants.isUser(role) && !RoleConstants.isAssistantAdmin(target.getRole()) && !RoleConstants.isGuest(target.getRole())) {
            throw new RuntimeException("仅支持协管员降级或游客转为普通用户");
        }
        target.setRole(role);
        sysUserService.updateById(target);
        return Result.ok();
    }

    @Operation(summary = "更新用户资料", description = "支持更新姓名、昵称、手机号、邮箱、头像")
    @PutMapping("/{id}/profile")
    public Result<SysUser> updateUserProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
        if (request == null) throw new RuntimeException("请求参数不能为空");
        SysUser user = sysUserService.getById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        ensureManageableTarget(user);
        applyProfilePatch(user, request);
        sysUserService.updateById(user);
        SysUser latest = sysUserService.getById(id);
        return Result.ok(maskSensitive(latest));
    }

    private void ensureManageableTarget(SysUser target) {
        if (RoleConstants.isSuperAdmin(target.getRole())) {
            throw new RuntimeException("超级管理员账号不可操作");
        }
        if (SecurityUtils.hasRole(RoleConstants.ROLE_SUPER_ADMIN)) {
            return;
        }
        if (RoleConstants.isAssistantAdmin(target.getRole())) {
            throw new RuntimeException("协管员不能操作其他协管员");
        }
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

    private UpdateProfileRequest toProfileRequest(CreateUserRequest request) {
        UpdateProfileRequest profile = new UpdateProfileRequest();
        profile.setRealName(request.getRealName());
        profile.setNickname(request.getNickname());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        profile.setAvatar(request.getAvatar());
        return profile;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
