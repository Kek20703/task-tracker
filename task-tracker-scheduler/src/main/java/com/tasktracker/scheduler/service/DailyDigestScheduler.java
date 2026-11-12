package com.tasktracker.scheduler.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyDigestScheduler {

    private final DigestService digestService;

    public DailyDigestScheduler(DigestService digestService) {
        this.digestService = digestService;
    }

    @Scheduled(cron = "${scheduler.cron:0 0 0 * * *}", zone = "${scheduler.zone:UTC}")
    public void run() {
        digestService.runForPreviousDay();
    }
}
