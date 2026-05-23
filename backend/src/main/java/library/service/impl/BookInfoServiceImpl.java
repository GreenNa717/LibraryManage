package library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import library.common.RoleConstants;
import library.dto.CopyLookupView;
import library.entity.BookInfo;
import library.entity.BookCopy;
import library.entity.ShelfLocation;
import library.entity.SysUser;
import library.mapper.BookInfoMapper;
import library.mapper.SysUserMapper;
import library.service.BookCopyService;
import library.service.BookInfoService;
import library.service.IsbnMetadataService;
import library.service.ShelfLocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookInfoServiceImpl extends ServiceImpl<BookInfoMapper, BookInfo> implements BookInfoService {

    private final SysUserMapper sysUserMapper;
    private final BookCopyService bookCopyService;
    private final ShelfLocationService shelfLocationService;
    private final IsbnMetadataService isbnMetadataService;

    public BookInfoServiceImpl(SysUserMapper sysUserMapper, BookCopyService bookCopyService,
                               ShelfLocationService shelfLocationService, IsbnMetadataService isbnMetadataService) {
        this.sysUserMapper = sysUserMapper;
        this.bookCopyService = bookCopyService;
        this.shelfLocationService = shelfLocationService;
        this.isbnMetadataService = isbnMetadataService;
    }

    @Override
    public Page<BookInfo> getPage(Page<BookInfo> page, String keyword, Long categoryId) {
        return getPage(page, keyword, categoryId, null, null, null, null, null);
    }

    @Override
    public Page<BookInfo> getPage(Page<BookInfo> page, String keyword, Long categoryId, Long locationId, Boolean lostOnly) {
        return getPage(page, keyword, categoryId, locationId, lostOnly, null, null, null);
    }

    @Override
    public Page<BookInfo> getPage(Page<BookInfo> page, String keyword, Long categoryId, Long locationId,
                                  Boolean lostOnly, String author, String isbn, Boolean availableOnly) {
        LambdaQueryWrapper<BookInfo> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            String normalizedKw = normalizeIsbn(kw);
            wrapper.and(w -> w.like(BookInfo::getTitle, kw)
                    .or()
                    .like(BookInfo::getAuthor, kw)
                    .or()
                    .like(BookInfo::getIsbn, kw)
                    .or()
                    .apply(!normalizedKw.isBlank(), "REPLACE(REPLACE(isbn, '-', ''), ' ', '') LIKE CONCAT('%', {0}, '%')", normalizedKw));
        }
        if (author != null && !author.isBlank()) {
            wrapper.eq(BookInfo::getAuthor, author.trim());
        }
        if (isbn != null && !isbn.isBlank()) {
            wrapper.apply("REPLACE(REPLACE(isbn, '-', ''), ' ', '') = {0}", normalizeIsbn(isbn));
        }
        if (categoryId != null) {
            wrapper.eq(BookInfo::getCategoryId, categoryId);
        }
        if (locationId != null) {
            wrapper.eq(BookInfo::getLocationId, locationId);
        }
        if (Boolean.TRUE.equals(lostOnly)) {
            wrapper.apply("EXISTS (SELECT 1 FROM book_copy bc WHERE bc.book_info_id = id AND bc.status = 2)");
        }
        if (Boolean.TRUE.equals(availableOnly)) {
            wrapper.gt(BookInfo::getCurrentStock, 0);
        }
        wrapper.orderByDesc(BookInfo::getId);
        return page(page, wrapper);
    }

    @Override
    public BookInfo findByIsbn(String isbn) {
        String normalized = normalizeIsbn(isbn);
        return getOne(new LambdaQueryWrapper<BookInfo>()
                .apply("REPLACE(REPLACE(isbn, '-', ''), ' ', '') = {0}", normalized)
                .last("LIMIT 1"));
    }

    private String normalizeIsbn(String value) {
        return value == null ? "" : value.replace("-", "").replace(" ", "").trim();
    }

    @Override
    public Long getTotalBooks() {
        return baseMapper.selectCount(null);
    }

    @Override
    public Long getTotalCopies() {
        List<BookInfo> books = baseMapper.selectList(null);
        return books.stream().mapToLong(BookInfo::getCurrentStock).sum();
    }

    @Override
    public Long getReaderCount() {
        return sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, RoleConstants.USER));
    }

    @Override
    public List<Map<String, Object>> getHotBooks(Integer limit) {
        return baseMapper.selectHotBooks(limit);
    }

    @Override
    public List<Map<String, Object>> getBookStocks() {
        List<BookInfo> books = list(new LambdaQueryWrapper<BookInfo>()
                .select(BookInfo::getId, BookInfo::getTitle, BookInfo::getTotalStock, BookInfo::getCurrentStock)
                .orderByAsc(BookInfo::getId));
        return books.stream().map(b -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", b.getId());
            map.put("title", b.getTitle());
            map.put("totalStock", b.getTotalStock());
            map.put("currentStock", b.getCurrentStock());
            return map;
        }).toList();
    }

    @Override
    public Map<String, Object> getInventoryHealth() {
        List<BookCopy> allCopies = bookCopyService.list();
        long available = 0, borrowed = 0, lost = 0;
        for (BookCopy c : allCopies) {
            switch (c.getStatus()) {
                case 0 -> available++;
                case 1 -> borrowed++;
                case 2 -> lost++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", available);
        result.put("borrowed", borrowed);
        result.put("lost", lost);
        return result;
    }

    @Override
    public List<Map<String, Object>> getLowStockBooks(int threshold) {
        List<BookInfo> books = list(new LambdaQueryWrapper<BookInfo>()
                .le(BookInfo::getCurrentStock, threshold)
                .gt(BookInfo::getCurrentStock, 0)
                .orderByAsc(BookInfo::getCurrentStock));
        return books.stream().map(b -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", b.getId());
            map.put("title", b.getTitle());
            map.put("isbn", b.getIsbn());
            map.put("totalStock", b.getTotalStock());
            map.put("currentStock", b.getCurrentStock());
            return map;
        }).toList();
    }

    @Override
    public Map<String, Object> findByCopyCode(String copyCode) {
        BookCopy copy = bookCopyService.findByCopyCode(copyCode);
        if (copy == null) return null;
        BookInfo book = getById(copy.getBookInfoId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("copyId", copy.getId());
        result.put("copyCode", copy.getCopyCode());
        result.put("copyStatus", copy.getStatus());
        if (book != null) {
            result.put("bookId", book.getId());
            result.put("title", book.getTitle());
            result.put("author", book.getAuthor());
            result.put("isbn", book.getIsbn());
            result.put("currentStock", book.getCurrentStock());
        }
        return result;
    }

    @Override
    public CopyLookupView findCopyLookupView(String copyCode) {
        BookCopy copy = bookCopyService.findByCopyCode(copyCode);
        if (copy == null) return null;
        BookInfo book = getById(copy.getBookInfoId());
        CopyLookupView view = new CopyLookupView();
        view.setCopyId(copy.getId());
        view.setCopyCode(copy.getCopyCode());
        view.setCopyStatus(copy.getStatus());
        view.setCopyStatusText(copy.getStatus() == 0 ? "在馆可借" : copy.getStatus() == 1 ? "已借出" : "丢失");
        if (book != null) {
            view.setBookId(book.getId());
            view.setTitle(book.getTitle());
            view.setAuthor(book.getAuthor());
            view.setIsbn(book.getIsbn());
            view.setCoverImage(book.getCoverImage());
            view.setCurrentStock(book.getCurrentStock());
        }
        boolean borrowable = copy.getStatus() == 0 && book != null && book.getCurrentStock() > 0;
        boolean returnable = copy.getStatus() == 1;
        view.setBorrowable(borrowable);
        view.setReturnable(returnable);
        if (borrowable) {
            view.setMessage("可借阅");
        } else if (copy.getStatus() == 1) {
            view.setMessage("该副本已借出");
        } else if (copy.getStatus() == 2) {
            view.setMessage("该副本已丢失");
        } else if (book != null && book.getCurrentStock() <= 0) {
            view.setMessage("图书暂无可借库存");
        } else {
            view.setMessage("暂不可借");
        }
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanInbound(String isbn, String title, String author, Long categoryId,
                                           Long locationId, String coverImage, String description, Integer quantity) {
        int inboundQty = quantity == null || quantity <= 0 ? 1 : quantity;
        BookInfo existing = findByIsbn(isbn);
        boolean created = false;
        BookInfo target;

        if (existing == null) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("新书入库时书名不能为空");
            }
            BookInfo book = new BookInfo();
            book.setIsbn(isbn);
            book.setTitle(title);
            book.setAuthor(author);
            book.setCategoryId(categoryId);
            book.setLocationId(locationId);
            book.setCoverImage(coverImage);
            book.setDescription(description);
            book.setTotalStock(inboundQty);
            book.setCurrentStock(inboundQty);
            save(book);
            target = book;
            created = true;
        } else {
            existing.setTotalStock((existing.getTotalStock() == null ? 0 : existing.getTotalStock()) + inboundQty);
            existing.setCurrentStock((existing.getCurrentStock() == null ? 0 : existing.getCurrentStock()) + inboundQty);
            if (locationId != null) {
                existing.setLocationId(locationId);
            }
            if (categoryId != null) {
                existing.setCategoryId(categoryId);
            }
            updateById(existing);
            target = existing;
        }

        List<BookCopy> copies = bookCopyService.generateCopies(target.getId(), target.getIsbn(), inboundQty, target.getLocationId());
        isbnMetadataService.saveLocalMetadata(target.getIsbn(), target.getTitle(), target.getAuthor(), target.getCoverImage());

        if (target.getLocationId() != null) {
            ShelfLocation location = shelfLocationService.getById(target.getLocationId());
            if (location != null) {
                int current = location.getCurrentCount() == null ? 0 : location.getCurrentCount();
                location.setCurrentCount(current + inboundQty);
                shelfLocationService.updateById(location);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("bookId", target.getId());
        result.put("isbn", target.getIsbn());
        result.put("title", target.getTitle());
        result.put("quantity", inboundQty);
        result.put("totalStock", target.getTotalStock());
        result.put("currentStock", target.getCurrentStock());
        result.put("copyCodes", copies.stream().map(BookCopy::getCopyCode).toList());
        return result;
    }
}
