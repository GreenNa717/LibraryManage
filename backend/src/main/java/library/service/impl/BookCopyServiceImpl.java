package library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import library.entity.BookCopy;
import library.mapper.BookCopyMapper;
import library.service.BookCopyService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookCopyServiceImpl extends ServiceImpl<BookCopyMapper, BookCopy> implements BookCopyService {

    @Override
    public List<BookCopy> generateCopies(Long bookInfoId, String isbn, int quantity, Long locationId) {
        List<BookCopy> copies = new ArrayList<>();
        List<BookCopy> existingCopies = list(new LambdaQueryWrapper<BookCopy>()
                .eq(BookCopy::getBookInfoId, bookInfoId));
        int startSeq = existingCopies.stream()
                .map(BookCopy::getCopyCode)
                .filter(StringUtils::isNotBlank)
                .mapToInt(code -> {
                    int idx = code.lastIndexOf('-');
                    if (idx < 0 || idx == code.length() - 1) return 0;
                    try {
                        return Integer.parseInt(code.substring(idx + 1));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        for (int i = 1; i <= quantity; i++) {
            BookCopy copy = new BookCopy();
            copy.setBookInfoId(bookInfoId);
            copy.setCopyCode(String.format("%s-%03d", isbn, startSeq + i));
            copy.setStatus(0);
            copy.setLocationId(locationId);
            copies.add(copy);
        }
        saveBatch(copies);
        return copies;
    }

    @Override
    public List<Map<String, Object>> getCopiesByBookId(Long bookInfoId) {
        List<BookCopy> copies = list(new LambdaQueryWrapper<BookCopy>()
                .eq(BookCopy::getBookInfoId, bookInfoId)
                .orderByAsc(BookCopy::getCopyCode));
        return copies.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("copyCode", c.getCopyCode());
            map.put("status", c.getStatus());
            map.put("locationId", c.getLocationId());
            map.put("createTime", c.getCreateTime());
            return map;
        }).toList();
    }

    @Override
    public BookCopy findByCopyCode(String copyCode) {
        return getOne(new LambdaQueryWrapper<BookCopy>().eq(BookCopy::getCopyCode, copyCode), false);
    }
}
