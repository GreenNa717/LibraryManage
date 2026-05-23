package library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import library.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    SysUser findByUsername(String username);
    Page<SysUser> getUserPage(Page<SysUser> page, String keyword);
}