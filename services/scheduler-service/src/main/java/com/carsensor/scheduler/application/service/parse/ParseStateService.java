package com.carsensor.scheduler.application.service.parse;

import java.time.LocalDateTime;
import java.util.List;
import com.carsensor.platform.dto.CarDto;

/**
 * Управление состоянием парсинга
 */
public interface ParseStateService {

    boolean isParsingInProgress();

    ParseStatus getLastParseStatus();

    void startParsing();

    void finishParsing(ParseResult result);

    void failParsing(String error);

    void stopParsing();

    record ParseStatus(
            boolean inProgress,
            LocalDateTime lastStartTime,
            LocalDateTime lastEndTime,
            int pagesParsed,
            int carsFound,
            int carsSaved,
            String lastError,
            ParseState state
    ) {
    }

    enum ParseState {
        IDLE, STARTING, PARSING_LIST, PARSING_DETAILS, SAVING, COMPLETED, FAILED, STOPPED
    }

    record ParseResult(
            LocalDateTime startTime,
            int pagesParsed,
            int carsFound,
            int carsSaved,
            List<CarDto> cars,
            String error
    ) {
    }
}