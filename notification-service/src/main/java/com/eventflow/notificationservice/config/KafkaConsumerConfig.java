package com.eventflow.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages:com.eventflow.*}")
    private String trustedPackages;

    @Value("${spring.kafka.consumer.properties.spring.json.use.type.headers:true}")
    private boolean useTypeHeaders;

    @Value("${spring.kafka.consumer.properties.spring.json.type.mapping:orderPlaced:com.eventflow.notificationservice.event.OrderPlacedEvent,orderCancelled:com.eventflow.notificationservice.event.OrderCancelledEvent}")
    private String typeMappings;

    @Value("${notification.retry.backoff-initial-ms:1000}")
    private long backoffInitialMs;

    @Value("${notification.retry.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    @Value("${notification.retry.max-attempts:3}")
    private int maxAttempts;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>();
        ErrorHandlingDeserializer<Object> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(valueDeserializer);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put("spring.json.trusted.packages", trustedPackages);
        props.put("spring.json.use.type.headers", useTypeHeaders);
        props.put("spring.json.type.mapping", typeMappings);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        // Exponential backoff: 1s → 2s → 4s (3 attempts), then DLT
        ExponentialBackOff backOff = new ExponentialBackOff(backoffInitialMs, backoffMultiplier);
        backOff.setMaxAttempts(maxAttempts);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
