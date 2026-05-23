package library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import library.entity.OperationLog;
import library.mapper.OperationLogMapper;
import library.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public void log(Long operatorId, String operatorName, String actionType, String targetDesc, String detail) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setActionType(actionType);
        log.setTargetDesc(targetDesc);
        log.setDetail(detail);
        save(log);
    }

    @Override
    public List<Map<String, Object>> getRecentLogs(int limit) {
        List<OperationLog> logs = list(new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getId)
                .last("LIMIT " + limit));
        return logs.stream().map(l -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", l.getId());
            map.put("operatorName", l.getOperatorName());
            map.put("actionType", l.getActionType());
            map.put("targetDesc", l.getTargetDesc());
            map.put("detail", l.getDetail());
            map.put("createTime", l.getCreateTime());
            return map;
        }).toList();
    }
}
