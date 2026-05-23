package library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import library.dto.BorrowRecordView;
import library.entity.BorrowRecord;

import java.util.List;
import java.util.Map;

public interface BorrowRecordService extends IService<BorrowRecord> {

    Page<BorrowRecord> getBorrowPage(Page<BorrowRecord> page, Integer status, Long userId, Long bookId,
                                     String bookTitle, String month, String startDate, String endDate);
    Page<BorrowRecordView> getBorrowViewPage(Page<BorrowRecord> page, Integer status, Long userId, Long bookId,
                                             String keyword, String bookTitle, String month,
                                             String startDate, String endDate);
    void createBorrow(Long userId, Long copyId);
    library.dto.BorrowActionResult createBorrowWithResult(Long userId, Long copyId);
    void doReturn(Long id);
    library.dto.ReturnActionResult doReturnWithResult(Long id);
    void refreshOverdueStatus();
    Long getBorrowingCount();
    Long getOverdueCount();
    Long getTodayBorrowCount();
    List<Map<String, Object>> getMonthlyBorrows();
    List<Map<String, Object>> getOverdueDetails();
    long countByUserIdAndStatus(Long userId, Integer status);
    long countByUserId(Long userId);
}
