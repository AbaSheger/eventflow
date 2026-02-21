package com.eventflow.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.topic.orders}")
    private String ordersTopic;

    @Value("${kafka.topic.orders-dlt}")
    private String ordersDltTopic;

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(ordersTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ordersDltTopic() {
        return TopicBuilder.name(ordersDltTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
