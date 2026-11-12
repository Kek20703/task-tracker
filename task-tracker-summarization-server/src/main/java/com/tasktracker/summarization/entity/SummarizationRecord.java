package com.tasktracker.summarization.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/** История запросов и готовых отчётов. */
@Table("summarization_requests")
public class SummarizationRecord {

    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @Version
    private Long version;

    @Column("request_id")
    private UUID requestId;

    @Column("user_id")
    private UUID userId;

    @Column("period_from")
    private Instant periodFrom;

    @Column("period_to")
    private Instant periodTo;

    @Column("task_count")
    private int taskCount;

    @Column("report")
    private String report;

    @Column("used_llm")
    private boolean usedLlm;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    protected SummarizationRecord() {
    }

    public SummarizationRecord(UUID requestId, UUID userId, Instant periodFrom, Instant periodTo,
                               int taskCount, String report, boolean usedLlm) {
        this.requestId = requestId;
        this.userId = userId;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.taskCount = taskCount;
        this.report = report;
        this.usedLlm = usedLlm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getPeriodFrom() {
        return periodFrom;
    }

    public Instant getPeriodTo() {
        return periodTo;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public String getReport() {
        return report;
    }

    public boolean isUsedLlm() {
        return usedLlm;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
