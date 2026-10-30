package com.kdu.csdtdas.backend.config;

import com.kdu.csdtdas.backend.service.TimetableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every hour, places the courses of lecturers who missed the timetable deadline. */
@Component
@EnableScheduling
public class TimetableDeadlineJob {

    private static final Logger log = LoggerFactory.getLogger(TimetableDeadlineJob.class);

    private final TimetableService service;

    public TimetableDeadlineJob(TimetableService service) {
        this.service = service;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 3_600_000)
    public void run() {
        try {
            service.enforceDeadlines();
        } catch (RuntimeException e) {
            log.warn("Timetable deadline check failed: {}", e.getMessage());
        }
    }
}