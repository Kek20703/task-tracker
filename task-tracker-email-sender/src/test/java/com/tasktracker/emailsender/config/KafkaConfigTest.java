package com.tasktracker.emailsender.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaConfigTest {

	private final KafkaConfig kafkaConfig = new KafkaConfig(3, 2000);

	@Test
	void tasksTopicIsCreatedWithSinglePartitionAndReplica() {
		NewTopic topic = kafkaConfig.emailSendingTasksTopic();

		assertThat(topic.name()).isEqualTo("EMAIL_SENDING_TASKS");
		assertThat(topic.numPartitions()).isEqualTo(1);
		assertThat(topic.replicationFactor()).isEqualTo((short) 1);
	}

	@Test
	void deadLetterTopicIsCreatedNextToTasksTopic() {
		NewTopic topic = kafkaConfig.emailSendingTasksDeadLetterTopic();

		assertThat(topic.name()).isEqualTo("EMAIL_SENDING_TASKS.DLT");
		assertThat(topic.numPartitions()).isEqualTo(1);
		assertThat(topic.replicationFactor()).isEqualTo((short) 1);
	}

	@Test
	void threeAttemptsMeanFirstTryPlusTwoRetries() {
		FixedBackOff backOff = KafkaConfig.retryPolicy(3, 2000);

		assertThat(backOff.getMaxAttempts()).isEqualTo(2);
		assertThat(backOff.getInterval()).isEqualTo(2000);
	}

	@Test
	void singleAttemptMeansNoRetries() {
		FixedBackOff backOff = KafkaConfig.retryPolicy(1, 500);

		assertThat(backOff.getMaxAttempts()).isZero();
		assertThat(backOff.getInterval()).isEqualTo(500);
	}

	@Test
	void nonsensicalAttemptCountDoesNotProduceNegativeRetries() {
		assertThat(KafkaConfig.retryPolicy(0, 500).getMaxAttempts()).isZero();
	}

	@Test
	void errorHandlerIsBuiltAroundRetryPolicy() {
		KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);

		assertThat(kafkaConfig.kafkaErrorHandler(kafkaTemplate)).isNotNull();
	}
}
