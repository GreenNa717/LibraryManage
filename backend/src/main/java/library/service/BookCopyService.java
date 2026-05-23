package library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import library.entity.BookCopy;

import java.util.List;
import java.util.Map;

public interface BookCopyService extends IService<BookCopy> {

    List<BookCopy> generateCopies(Long bookInfoId, String isbn, int quantity, Long locationId);
    List<Map<String, Object>> getCopiesByBookId(Long bookInfoId);
    BookCopy findByCopyCode(String copyCode);
}
