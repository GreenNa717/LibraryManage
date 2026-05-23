package library.service;

import library.common.JwtUtil;
import library.dto.LoginResponse;
import library.entity.SysUser;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SysUserService sysUserService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(SysUserService sysUserService, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(String username, String password) {
        return login(username, password, null);
    }

    public LoginResponse login(String username, String password, Integer allowedRole) {
        SysUser user = sysUserService.findByUsername(username);
        if (user == null) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (!passwordMatches(password, user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被封禁");
        }
        if (allowedRole != null && !allowedRole.equals(user.getRole())) {
            throw new RuntimeException("该账号无法通过此入口登录");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getNickname(),
                user.getAvatar(),
                token
        );
    }

    public boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, storedPassword);
    }
}
