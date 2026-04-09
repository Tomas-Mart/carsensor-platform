package com.carsensor.scheduler.application.service.parse.impl;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.parse.ParseStateService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ParseStateServiceImpl implements ParseStateService {

    private final AtomicBoolean parsingInProgress = new AtomicBoolean(false);
    private final AtomicReference<ParseStatus> currentStatus = new AtomicReference<>(
            new ParseStatus(false, null, null, 0, 0, 0, null, ParseState.IDLE)
    );

    @Override
    public boolean isParsingInProgress() {
        return parsingInProgress.get();
    }

    @Override
    public ParseStatus getLastParseStatus() {
        return currentStatus.get();
    }

    @Override
    public void startParsing() {
        parsingInProgress.set(true);
        updateStatus(true, LocalDateTime.now(), null, 0, 0, 0, null, ParseState.STARTING);
        log.debug("Parsing started");
    }

    @Override
    public void finishParsing(ParseResult result) {
        updateStatus(false, result.startTime(), LocalDateTime.now(),
                result.pagesParsed(), result.carsFound(), result.carsSaved(),
                result.error(), ParseState.COMPLETED);
        parsingInProgress.set(false);
        log.info("Parsing finished. Pages: {}, Cars found: {}, Cars saved: {}",
                result.pagesParsed(), result.carsFound(), result.carsSaved());
    }

    @Override
    public void failParsing(String error) {
        ParseStatus status = currentStatus.get();
        updateStatus(false, status.lastStartTime(), LocalDateTime.now(),
                status.pagesParsed(), status.carsFound(), status.carsSaved(),
                error, ParseState.FAILED);
        parsingInProgress.set(false);
        log.error("Parsing failed: {}", error);
    }

    @Override
    public void stopParsing() {
        if (parsingInProgress.get()) {
            log.info("Stopping current parsing process");
            ParseStatus status = currentStatus.get();
            updateStatus(false, status.lastStartTime(), LocalDateTime.now(),
                    status.pagesParsed(), status.carsFound(), status.carsSaved(),
                    "Stopped by user", ParseState.STOPPED);
            parsingInProgress.set(false);
        }
    }

    // Вспомогательный приватный метод (не в интерфейсе)
    private void updateStatus(
            boolean inProgress, LocalDateTime startTime, LocalDateTime endTime,
            int pagesParsed, int carsFound, int carsSaved,
            String error, ParseState state
    ) {
        currentStatus.set(new ParseStatus(
                inProgress, startTime, endTime,
                pagesParsed, carsFound, carsSaved, error, state
        ));
        log.debug("Status updated: state={}, inProgress={}", state, inProgress);
    }
}