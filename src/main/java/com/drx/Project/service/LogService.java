package com.drx.Project.service;

import com.drx.Project.model.MyLog;
import com.drx.Project.repositories.LogRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    public void saveLogToDb(MyLog myLog) {
        logRepository.save(myLog);
    }

    public List<MyLog> getItemLogsById(int itemId) {
        return logRepository.findAllByItemId(itemId);
    }


}
