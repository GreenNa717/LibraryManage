package library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import library.entity.BookInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BookInfoMapper extends BaseMapper<BookInfo> {

    List<Map<String, Object>> selectHotBooks(@Param("limit") Integer limit);
}