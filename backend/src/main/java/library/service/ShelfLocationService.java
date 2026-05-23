package library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import library.entity.ShelfLocation;

import java.util.List;
import java.util.Map;

public interface ShelfLocationService extends IService<ShelfLocation> {

    List<Map<String, Object>> getCapacityData();
}
