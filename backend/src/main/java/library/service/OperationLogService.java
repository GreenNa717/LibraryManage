package library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import library.entity.OperationLog;

import java.util.List;
import java.util.Map;

public interface OperationLogService extends IService<OperationLog> {

    void log(Long operatorId, String operatorName, String actionType, String targetDesc, String detail);
    List<Map<String, Object>> getRecentLogs(int limit);
}
