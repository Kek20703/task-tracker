package com.tasktracker.summarization.repository;

import com.tasktracker.summarization.entity.SummarizationRecord;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface SummarizationRecordRepository extends CrudRepository<SummarizationRecord, UUID> {

    Optional<SummarizationRecord> findByRequestId(UUID requestId);
}
