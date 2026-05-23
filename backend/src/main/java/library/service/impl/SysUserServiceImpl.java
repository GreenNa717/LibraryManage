package library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import library.entity.SysUser;
import library.mapper.SysUserMapper;
import library.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<>(SysUser.class).eq(SysUser::getUsername, username));
    }

    @Override
    public Page<SysUser> getUserPage(Page<SysUser> page, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getEmail, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.orderByDesc(SysUser::getId);
        return page(page, wrapper);
    }
}
