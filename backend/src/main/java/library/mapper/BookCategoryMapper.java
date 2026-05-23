package library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import library.entity.BookCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookCategoryMapper extends BaseMapper<BookCategory> {
}