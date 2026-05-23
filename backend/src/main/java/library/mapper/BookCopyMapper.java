package library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import library.entity.BookCopy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookCopyMapper extends BaseMapper<BookCopy> {
}
