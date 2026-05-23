package library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import library.entity.BookCategory;
import library.entity.ShelfLocation;
import library.mapper.ShelfLocationMapper;
import library.service.BookCategoryService;
import library.service.ShelfLocationService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShelfLocationServiceImpl extends ServiceImpl<ShelfLocationMapper, ShelfLocation> implements ShelfLocationService {

    private final BookCategoryService bookCategoryService;

    public ShelfLocationServiceImpl(BookCategoryService bookCategoryService) {
        this.bookCategoryService = bookCategoryService;
    }

    @Override
    public List<Map<String, Object>> getCapacityData() {
        List<ShelfLocation> locations = list(new LambdaQueryWrapper<ShelfLocation>()
                .orderByAsc(ShelfLocation::getId));
        return locations.stream().map(loc -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", loc.getId());
            map.put("locationName", loc.getLocationName());
            map.put("maxCapacity", loc.getMaxCapacity());
            map.put("currentCount", loc.getCurrentCount());
            int rate = loc.getMaxCapacity() > 0
                    ? (int) Math.round(loc.getCurrentCount() * 100.0 / loc.getMaxCapacity())
                    : 0;
            map.put("rate", rate);
            BookCategory cat = loc.getCategoryId() != null ? bookCategoryService.getById(loc.getCategoryId()) : null;
            map.put("categoryName", cat != null ? cat.getCategoryName() : "-");
            return map;
        }).toList();
    }
}
