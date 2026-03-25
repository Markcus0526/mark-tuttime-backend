package com.tuttime.common.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Generic Kafka event producer for publishing events to Kafka topics
 * <p>
 * Note: This component is only available when KafkaTemplate is configured.
 * Services should check for its availability before using it, or handle gracefully when Kafka is disabled.
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaTemplate.class)
public class EventProducer {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final KafkaMetrics kafkaMetrics; // Optional - will be null if not available

	public EventProducer(KafkaTemplate<String, String> kafkaTemplate,
	                     ObjectMapper objectMapper,
	                     KafkaMetrics kafkaMetrics) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.kafkaMetrics = kafkaMetrics; // Can be null if Micrometer not available
	}

	// Topic names
	private static final String TOPIC_USERS = "tuttime.users";
	private static final String TOPIC_RESTAURANTS = "tuttime.restaurants";
	private static final String TOPIC_MINING = "tuttime.mining";
	private static final String TOPIC_STAKING = "tuttime.staking";
	private static final String TOPIC_CONVERSION = "tuttime.conversion";
	private static final String TOPIC_REWARDS = "tuttime.rewards";
	private static final String TOPIC_TOKENS = "tuttime.tokens";
	private static final String TOPIC_BOOKINGS = "apart-sharing.booking";
	private static final String TOPIC_RENTALS = "apart-sharing.rental";
	private static final String TOPIC_USER_BLOCKS = "tuttime.user-blocks";
	private static final String TOPIC_REFERRALS = "tuttime.referrals";

	/**
	 * Publish user event
	 */
	public void publishUserEvent(UserEvent event) {
		publishEvent(TOPIC_USERS, event.getUserId().toString(), event);
	}

	/**
	 * Publish user event
	 */
	public void publishRestaurantEvent(RestaurantEvent event) {
		publishEvent(TOPIC_RESTAURANTS, event.getRestaurantId().toString(), event);
	}

	/**
	 * Publish mining event
	 */
	public void publishMiningEvent(MiningEvent event) {
		publishEvent(TOPIC_MINING, event.getUserId().toString(), event);
	}

	/**
	 * Publish staking event
	 */
	public void publishStakingEvent(StakingEvent event) {
		publishEvent(TOPIC_STAKING, event.getUserId().toString(), event);
	}

	/**
	 * Publish conversion event
	 */
	public void publishConversionEvent(ConversionEvent event) {
		publishEvent(TOPIC_CONVERSION, event.getUserId().toString(), event);
	}

	/**
	 * Publishes a reward event to the rewards topic.
	 *
	 * @param event The reward event to publish
	 */
	public void publishRewardEvent(RewardEvent event) {
		publishEvent(TOPIC_REWARDS, event.getUserId().toString(), event);
	}

	/**
	 * Publish token event
	 */
	public void publishTokenEvent(TokenEvent event) {
		publishEvent(TOPIC_TOKENS, event.getUserId().toString(), event);
	}

	/**
	 * Publish booking event
	 */
	public void publishBookingEvent(BookingEvent event) {
		publishEvent(TOPIC_BOOKINGS + "." + event.getEventType().toLowerCase(),
				event.getUserId().toString(), event);
	}

	/**
	 * Publish rental event
	 */
	public void publishRentalEvent(RentalEvent event) {
		publishEvent(TOPIC_RENTALS + "." + event.getEventType().toLowerCase(),
				event.getUserId().toString(), event);
	}

	/**
	 * Publish user block event.
	 *
	 * <p>This event is published when a user blocks or unblocks another user.
	 * Multiple services can consume this event to sync blocking state.
	 *
	 * @param event The user block event
	 */
	public void publishUserBlockEvent(UserBlockEvent event) {
		// Use blocker_user_id as partition key to ensure ordering per blocker
		String partitionKey = event.getBlockerUserId() != null
				? event.getBlockerUserId().toString()
				: "unknown";
		publishEvent(TOPIC_USER_BLOCKS, partitionKey, event);
	}

	/**
	 * Publish referral event.
	 *
	 * <p>This event is published when a user signs up with a referral code.
	 * The Rewards Service consumes this event to validate the referral code
	 * and create the referral relationship.
	 *
	 * @param event The referral event
	 */
	public void publishReferralEvent(ReferralEvent event) {
		// Use referee_id as partition key to ensure ordering per new user
		String partitionKey = event.getRefereeId() != null
				? event.getRefereeId().toString()
				: "unknown";
		publishEvent(TOPIC_REFERRALS, partitionKey, event);
	}

	/**
	 * Generic event publisher
	 *
	 * <p><b>Timezone Note:</b> All timestamps are set using OffsetDateTime.now(ZoneOffset.UTC)
	 * to ensure UTC timezone consistency. All Kafka event timestamps are in UTC.
	 */
	private void publishEvent(String topic, String key, Object event) {
		try {
			// Set timestamp if not set (uses UTC timezone explicitly)
			if (event instanceof UserEvent userEvent) {
				if (userEvent.getTimestamp() == null) {
					userEvent.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
				}
			} else if (event instanceof MiningEvent miningEvent) {
				if (miningEvent.getTimestamp() == null) {
					miningEvent.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
				}
			} else if (event instanceof StakingEvent stakingEvent) {
				if (stakingEvent.getTimestamp() == null) {
					stakingEvent.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
				}
			} else if (event instanceof ConversionEvent conversionEvent) {
				if (conversionEvent.getTimestamp() == null) {
					conversionEvent.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
				}
			} else if (event instanceof TokenEvent tokenEvent) {
				if (tokenEvent.getTimestamp() == null) {
					tokenEvent.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
				}
			} else if (event instanceof UserBlockEvent userBlockEvent) {
				if (userBlockEvent.getTimestamp() == null) {
					userBlockEvent.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
				}
			} else if (event instanceof ReferralEvent referralEvent) {
				if (referralEvent.getTimestamp() == null) {
					referralEvent.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
				}
			}

			String message = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(topic, key, message).whenComplete((result, ex) -> {
				if (ex == null && kafkaMetrics != null) {
					kafkaMetrics.recordProducerMessageSent(topic, true);
				} else if (ex != null) {
					String stackTraceSummary = getStackTraceSummary(ex, 3);
					log.error("Error publishing event to topic {}: {}, stackTrace={}", topic, ex.getMessage(), stackTraceSummary);
					if (kafkaMetrics != null) {
						kafkaMetrics.recordProducerMessageSent(topic, false);
						kafkaMetrics.recordProducerError(topic, ex.getClass().getSimpleName());
					}
				}
			});
			log.info("Published event to topic {}: key={}, type={}",
					topic, key, event.getClass().getSimpleName());
		} catch (JsonProcessingException e) {
			log.error("Error serializing event for topic {}: {}", topic, e.getMessage(), e);
			if (kafkaMetrics != null) {
				kafkaMetrics.recordProducerError(topic, e.getClass().getSimpleName());
			}
		} catch (Exception e) {
			log.error("Error publishing event to topic {}: {}", topic, e.getMessage(), e);
			if (kafkaMetrics != null) {
				kafkaMetrics.recordProducerError(topic, e.getClass().getSimpleName());
			}
		}
	}

	/**
	 * Gets a shortened stack trace summary for logging.
	 *
	 * @param ex       The exception
	 * @param maxLines Maximum number of stack trace lines to include
	 * @return Stack trace summary string (first N lines)
	 */
	private String getStackTraceSummary(Throwable ex, int maxLines) {
		if (ex == null) {
			return "";
		}

		StackTraceElement[] stackTrace = ex.getStackTrace();
		if (stackTrace == null || stackTrace.length == 0) {
			return "";
		}

		StringBuilder summary = new StringBuilder();
		int linesToInclude = Math.min(maxLines, stackTrace.length);

		for (int i = 0; i < linesToInclude; i++) {
			if (i > 0) {
				summary.append(" | ");
			}
			summary.append(stackTrace[i].toString());
		}

		if (stackTrace.length > maxLines) {
			summary.append(" ... (").append(stackTrace.length - maxLines).append(" more)");
		}

		return summary.toString();
	}
}

