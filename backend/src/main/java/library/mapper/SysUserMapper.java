package library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import library.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}