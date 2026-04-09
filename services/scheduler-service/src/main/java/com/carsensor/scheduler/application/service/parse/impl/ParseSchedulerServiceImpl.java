package com.carsensor.scheduler.application.service.parse.impl;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;
import com.carsensor.scheduler.application.service.parse.ParseSchedulerService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ParseSchedulerServiceImpl implements ParseSchedulerService {

    private final AtomicBoolean scheduledRunning = new AtomicBoolean(false);
    private String currentCronExpression;

    @Override
    public void scheduleParsing(String cronExpression) {
        log.info("Scheduling parsing with cron: {}", cronExpression);
        this.currentCronExpression = cronExpression;
        // В реальном проекте здесь должна быть динамическая настройка расписания
        // Например, через TaskScheduler или Quartz
        throw new UnsupportedOperationException("Dynamic scheduling not implemented yet");
    }

    @Override
    public void startScheduledParsing() {
        if (!scheduledRunning.get()) {
            scheduledRunning.set(true);
            log.info("Scheduled parsing started");
        }
    }

    @Override
    public void stopScheduledParsing() {
        if (scheduledRunning.get()) {
            scheduledRunning.set(false);
            log.info("Scheduled parsing stopped");
        }
    }

    @Override
    public String getCurrentSchedule() {
        return currentCronExpression;
    }

    @Override
    public boolean isScheduledRunning() {
        return scheduledRunning.get();
    }
}