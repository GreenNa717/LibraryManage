package library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import library.entity.BookInfo;

import java.util.List;
import java.util.Map;

public interface BookInfoService extends IService<BookInfo> {

    Page<BookInfo> getPage(Page<BookInfo> page, String keyword, Long categoryId);
    Page<BookInfo> getPage(Page<BookInfo> page, String keyword, Long categoryId, Long locationId, Boolean lostOnly);
    Page<BookInfo> getPage(Page<BookInfo> page, String keyword, Long categoryId, Long locationId,
                           Boolean lostOnly, String author, String isbn, Boolean availableOnly);
    BookInfo findByIsbn(String isbn);
    Long getTotalBooks();
    Long getTotalCopies();
    Long getReaderCount();
    List<Map<String, Object>> getHotBooks(Integer limit);
    List<Map<String, Object>> getBookStocks();
    Map<String, Object> getInventoryHealth();
    List<Map<String, Object>> getLowStockBooks(int threshold);
    Map<String, Object> findByCopyCode(String copyCode);
    library.dto.CopyLookupView findCopyLookupView(String copyCode);
    Map<String, Object> scanInbound(String isbn, String title, String author, Long categoryId,
                                    Long locationId, String coverImage, String description, Integer quantity);
}
