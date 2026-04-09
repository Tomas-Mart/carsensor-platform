package com.carsensor.scheduler.application.service.parse.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.parse.ParseHistoryService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ParseHistoryServiceImpl implements ParseHistoryService {

    private final ConcurrentLinkedQueue<ParseHistoryRecord> parseHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_HISTORY_SIZE = 100;

    @Override
    public void addRecord(ParseHistoryRecord record) {
        parseHistory.offer(record);

        while (parseHistory.size() > MAX_HISTORY_SIZE) {
            parseHistory.poll();
        }
        log.debug("Added parse history record. Total records: {}", parseHistory.size());
    }

    @Override
    public List<ParseHistoryRecord> getLastRecords(int limit) {
        return parseHistory.stream()
                .limit(limit)
                .toList();
    }

    @Override
    public void clearHistory() {
        parseHistory.clear();
        log.info("Parse history cleared");
    }

    @Override
    public ParseHistoryRecord createRecord(
            LocalDateTime startTime, LocalDateTime endTime,
            int pagesParsed, int carsFound, int carsSaved,
            boolean success, String errorMessage
    ) {
        return new ParseHistoryRecord(startTime, endTime, pagesParsed, carsFound, carsSaved, success, errorMessage);
    }
}