package com.tasktracker.scheduler.config;

import com.tasktracker.scheduler.service.DigestService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Ручной запуск рассылки при старте: {@code SCHEDULER_RUN_ON_STARTUP=true}. */
@Component
@ConditionalOnProperty(prefix = "scheduler", name = "run-on-startup", havingValue = "true")
public class ManualDigestRunner implements ApplicationRunner {

    private final DigestService digestService;

    public ManualDigestRunner(DigestService digestService) {
        this.digestService = digestService;
    }

    @Override
    public void run(ApplicationArguments args) {
        digestService.runForPreviousDay();
    }
}
