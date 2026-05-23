package library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import library.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    List<Map<String, Object>> selectMonthlyBorrows();
}