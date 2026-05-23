package library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import library.common.RoleConstants;
import library.dto.BorrowActionResult;
import library.dto.BorrowRecordView;
import library.dto.ReturnActionResult;
import library.entity.BookCopy;
import library.entity.BookInfo;
import library.entity.BorrowRecord;
import library.entity.SysUser;
import library.mapper.BorrowRecordMapper;
import library.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BorrowRecordServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowRecordService {

    private final BookInfoService bookInfoService;
    private final BookCopyService bookCopyService;
    private final SysUserService sysUserService;
    private final OperationLogService operationLogService;

    public BorrowRecordServiceImpl(BookInfoService bookInfoService,
                                   BookCopyService bookCopyService,
                                   SysUserService sysUserService,
                                   OperationLogService operationLogService) {
        this.bookInfoService = bookInfoService;
        this.bookCopyService = bookCopyService;
        this.sysUserService = sysUserService;
        this.operationLogService = operationLogService;
    }

    @Override
    public Page<BorrowRecord> getBorrowPage(Page<BorrowRecord> page, Integer status, Long userId, Long bookId,
                                            String bookTitle, String month, String startDate, String endDate) {
        LambdaQueryWrapper<BorrowRecord> wrapper = buildBorrowQuery(status, userId, bookId, null,
                bookTitle, month, startDate, endDate);
        return page(page, wrapper);
    }

    @Override
    public Page<BorrowRecordView> getBorrowViewPage(Page<BorrowRecord> page, Integer status, Long userId, Long bookId,
                                                    String keyword, String bookTitle, String month,
                                                    String startDate, String endDate) {
        LambdaQueryWrapper<BorrowRecord> wrapper = buildBorrowQuery(status, userId, bookId, keyword,
                bookTitle, month, startDate, endDate);
        Page<BorrowRecord> recordPage = page(page, wrapper);
        Page<BorrowRecordView> viewPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<BorrowRecord> records = recordPage.getRecords();
        Map<Long, SysUser> userMap = batchGetUsers(records);
        Map<Long, BookInfo> bookMap = batchGetBooks(records);
        Map<Long, BookCopy> copyMap = batchGetCopies(records);
        viewPage.setRecords(records.stream().map(r -> toBorrowRecordView(r, userMap, bookMap, copyMap)).toList());
        return viewPage;
    }

    private LambdaQueryWrapper<BorrowRecord> buildBorrowQuery(Integer status, Long userId, Long bookId,
                                                              String keyword, String bookTitle, String month,
                                                              String startDate, String endDate) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(BorrowRecord::getStatus, status);
        if (userId != null) wrapper.eq(BorrowRecord::getUserId, userId);
        if (bookId != null) wrapper.eq(BorrowRecord::getBookId, bookId);
        if (bookTitle != null && !bookTitle.isBlank()) {
            wrapper.apply("book_id IN (SELECT id FROM book_info WHERE title LIKE CONCAT('%', {0}, '%'))", bookTitle.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            String normalizedKw = normalizeIsbn(kw);
            Long keywordId = parseIdKeyword(kw);
            wrapper.and(w -> {
                if (keywordId != null) {
                    w.eq(BorrowRecord::getUserId, keywordId)
                            .or()
                            .eq(BorrowRecord::getBookId, keywordId)
                            .or()
                            .eq(BorrowRecord::getCopyId, keywordId)
                            .or();
                }
                w.apply("user_id IN (SELECT id FROM sys_user WHERE username LIKE CONCAT('%', {0}, '%') OR real_name LIKE CONCAT('%', {0}, '%') OR nickname LIKE CONCAT('%', {0}, '%') OR phone LIKE CONCAT('%', {0}, '%'))", kw)
                        .or()
                        .apply("book_id IN (SELECT id FROM book_info WHERE title LIKE CONCAT('%', {0}, '%') OR author LIKE CONCAT('%', {0}, '%') OR isbn LIKE CONCAT('%', {0}, '%') OR REPLACE(REPLACE(isbn, '-', ''), ' ', '') LIKE CONCAT('%', {1}, '%'))", kw, normalizedKw)
                        .or()
                        .apply("copy_id IN (SELECT id FROM book_copy WHERE copy_code LIKE CONCAT('%', {0}, '%'))", kw);
            });
        }
        if (month != null && !month.isBlank()) {
            wrapper.apply("DATE_FORMAT(borrow_time, '%Y-%m') = {0}", month.trim());
        }
        if (startDate != null && !startDate.isBlank()) {
            try {
                LocalDate start = LocalDate.parse(startDate.trim());
                wrapper.ge(BorrowRecord::getBorrowTime, start.atStartOfDay());
            } catch (Exception ignored) {
            }
        }
        if (endDate != null && !endDate.isBlank()) {
            try {
                LocalDate end = LocalDate.parse(endDate.trim());
                wrapper.lt(BorrowRecord::getBorrowTime, end.plusDays(1).atStartOfDay());
            } catch (Exception ignored) {
            }
        }
        wrapper.orderByDesc(BorrowRecord::getId);
        return wrapper;
    }

    private Long parseIdKeyword(String keyword) {
        try {
            return keyword.matches("\\d+") ? Long.parseLong(keyword) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeIsbn(String value) {
        return value == null ? "" : value.replace("-", "").replace(" ", "").trim();
    }

    private BorrowRecordView toBorrowRecordView(BorrowRecord record,
                                                Map<Long, SysUser> userMap,
                                                Map<Long, BookInfo> bookMap,
                                                Map<Long, BookCopy> copyMap) {
        BorrowRecordView view = new BorrowRecordView();
        view.setId(record.getId());
        view.setUserId(record.getUserId());
        view.setBookId(record.getBookId());
        view.setCopyId(record.getCopyId());
        view.setBorrowTime(record.getBorrowTime());
        view.setDueTime(record.getDueTime());
        view.setReturnTime(record.getReturnTime());
        view.setStatus(record.getStatus());
        view.setCreateTime(record.getCreateTime());

        SysUser user = record.getUserId() != null ? userMap.get(record.getUserId()) : null;
        if (user != null) {
            view.setUsername(user.getUsername());
            view.setRealName(user.getRealName());
            view.setNickname(user.getNickname());
            view.setPhone(user.getPhone());
        }

        BookInfo book = record.getBookId() != null ? bookMap.get(record.getBookId()) : null;
        if (book != null) {
            view.setBookTitle(book.getTitle());
            view.setBookAuthor(book.getAuthor());
            view.setIsbn(book.getIsbn());
            view.setCoverImage(book.getCoverImage());
        }

        BookCopy copy = record.getCopyId() != null ? copyMap.get(record.getCopyId()) : null;
        if (copy != null) {
            view.setCopyCode(copy.getCopyCode());
        }
        return view;
    }

    private Map<Long, SysUser> batchGetUsers(List<BorrowRecord> records) {
        List<Long> ids = records.stream().map(BorrowRecord::getUserId).filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return sysUserService.listByIds(ids).stream().collect(java.util.stream.Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, BookInfo> batchGetBooks(List<BorrowRecord> records) {
        List<Long> ids = records.stream().map(BorrowRecord::getBookId).filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return bookInfoService.listByIds(ids).stream().collect(java.util.stream.Collectors.toMap(BookInfo::getId, b -> b, (a, b2) -> a));
    }

    private Map<Long, BookCopy> batchGetCopies(List<BorrowRecord> records) {
        List<Long> ids = records.stream().map(BorrowRecord::getCopyId).filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return bookCopyService.listByIds(ids).stream().collect(java.util.stream.Collectors.toMap(BookCopy::getId, c -> c, (a, b) -> a));
    }

    @Override
    @Transactional
    public void createBorrow(Long userId, Long copyId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (user.getStatus() == 0) throw new RuntimeException("该用户已被封禁");
        if (!RoleConstants.isUser(user.getRole())) throw new RuntimeException("仅普通用户可借阅");

        BookCopy copy = bookCopyService.getById(copyId);
        if (copy == null) throw new RuntimeException("副本不存在");
        if (copy.getStatus() == 1) throw new RuntimeException("该副本已借出");
        if (copy.getStatus() == 2) throw new RuntimeException("该副本已丢失");

        BookInfo book = bookInfoService.getById(copy.getBookInfoId());
        if (book == null) throw new RuntimeException("图书不存在");
        if (book.getCurrentStock() <= 0) throw new RuntimeException("该图书库存不足");

        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(book.getId());
        record.setCopyId(copyId);
        record.setBorrowTime(LocalDateTime.now());
        record.setDueTime(LocalDateTime.now().plusDays(30));
        record.setStatus(0);
        save(record);

        copy.setStatus(1);
        bookCopyService.updateById(copy);

        book.setCurrentStock(book.getCurrentStock() - 1);
        bookInfoService.updateById(book);

        operationLogService.log(userId, user.getRealName(), "BORROW",
                "《" + book.getTitle() + "》 " + copy.getCopyCode(),
                "借出给 " + user.getRealName());
    }

    @Override
    @Transactional
    public BorrowActionResult createBorrowWithResult(Long userId, Long copyId) {
        createBorrow(userId, copyId);
        BorrowRecord record = getOne(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getCopyId, copyId)
                .orderByDesc(BorrowRecord::getId)
                .last("LIMIT 1"));
        BorrowActionResult result = new BorrowActionResult();
        if (record != null) {
            result.setRecordId(record.getId());
            result.setBookId(record.getBookId());
            result.setCopyId(record.getCopyId());
            result.setBorrowTime(record.getBorrowTime());
            result.setDueTime(record.getDueTime());
            result.setStatus(record.getStatus());
            BookInfo book = bookInfoService.getById(record.getBookId());
            if (book != null) result.setBookTitle(book.getTitle());
            BookCopy copy = bookCopyService.getById(record.getCopyId());
            if (copy != null) result.setCopyCode(copy.getCopyCode());
            result.setMessage("借阅成功，请于" + record.getDueTime().toLocalDate() + "前归还");
        }
        return result;
    }

    @Override
    @Transactional
    public void doReturn(Long id) {
        BorrowRecord record = getById(id);
        if (record == null) throw new RuntimeException("借阅记录不存在");
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new RuntimeException("该记录已归还，无需重复操作");
        }
        record.setStatus(1);
        record.setReturnTime(LocalDateTime.now());
        updateById(record);

        if (record.getCopyId() != null) {
            BookCopy copy = bookCopyService.getById(record.getCopyId());
            if (copy != null) {
                copy.setStatus(0);
                bookCopyService.updateById(copy);
            }
        }

        BookInfo book = bookInfoService.getById(record.getBookId());
        if (book != null) {
            book.setCurrentStock(book.getCurrentStock() + 1);
            bookInfoService.updateById(book);
        }

        SysUser user = sysUserService.getById(record.getUserId());
        String userName = user != null ? user.getRealName() : "用户" + record.getUserId();
        String bookName = book != null ? book.getTitle() : "图书" + record.getBookId();
        String copyCode = record.getCopyId() != null ? getCopyCode(record.getCopyId()) : "";
        operationLogService.log(record.getUserId(), userName, "RETURN",
                "《" + bookName + "》 " + copyCode,
                userName + " 归还");
    }

    @Override
    @Transactional
    public ReturnActionResult doReturnWithResult(Long id) {
        BorrowRecord before = getById(id);
        if (before == null) throw new RuntimeException("借阅记录不存在");
        doReturn(id);
        BorrowRecord after = getById(id);
        ReturnActionResult result = new ReturnActionResult();
        result.setRecordId(id);
        result.setStatus(after.getStatus());
        result.setReturnTime(after.getReturnTime());
        BookInfo book = bookInfoService.getById(before.getBookId());
        if (book != null) result.setBookTitle(book.getTitle());
        String copyCode = before.getCopyId() != null ? getCopyCode(before.getCopyId()) : "";
        result.setCopyCode(copyCode);
        result.setMessage("归还成功");
        return result;
    }

    private String getCopyCode(Long copyId) {
        BookCopy copy = bookCopyService.getById(copyId);
        return copy != null ? copy.getCopyCode() : "";
    }

    @Override
    public void refreshOverdueStatus() {
        List<BorrowRecord> records = list(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, 0)
                .lt(BorrowRecord::getDueTime, LocalDateTime.now()));
        for (BorrowRecord record : records) {
            record.setStatus(2);
            updateById(record);
        }
    }

    @Override
    public Long getBorrowingCount() {
        return count(new LambdaQueryWrapper<BorrowRecord>().eq(BorrowRecord::getStatus, 0));
    }

    @Override
    public Long getOverdueCount() {
        return count(new LambdaQueryWrapper<BorrowRecord>().eq(BorrowRecord::getStatus, 2));
    }

    @Override
    public Long getTodayBorrowCount() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return count(new LambdaQueryWrapper<BorrowRecord>().ge(BorrowRecord::getBorrowTime, startOfDay));
    }

    @Override
    public List<Map<String, Object>> getMonthlyBorrows() {
        return baseMapper.selectMonthlyBorrows();
    }

    @Override
    public List<Map<String, Object>> getOverdueDetails() {
        List<BorrowRecord> records = list(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, 2)
                .orderByDesc(BorrowRecord::getDueTime));
        return records.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("userId", r.getUserId());
            map.put("bookId", r.getBookId());
            map.put("copyId", r.getCopyId());
            map.put("borrowTime", r.getBorrowTime());
            map.put("dueTime", r.getDueTime());
            BookInfo book = bookInfoService.getById(r.getBookId());
            SysUser user = sysUserService.getById(r.getUserId());
            map.put("bookTitle", book != null ? book.getTitle() : "-");
            map.put("userName", user != null ? user.getRealName() : "-");
            if (r.getCopyId() != null) {
                BookCopy copy = bookCopyService.getById(r.getCopyId());
                map.put("copyCode", copy != null ? copy.getCopyCode() : "-");
            }
            return map;
        }).toList();
    }

    @Override
    public long countByUserIdAndStatus(Long userId, Integer status) {
        return count(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getStatus, status));
    }

    @Override
    public long countByUserId(Long userId) {
        return count(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId));
    }
}
