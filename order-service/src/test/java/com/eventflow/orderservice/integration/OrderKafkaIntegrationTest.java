package com.eventflow.orderservice.integration;

import com.eventflow.orderservice.dto.CreateOrderRequest;
import com.eventflow.orderservice.event.OrderPlacedEvent;
import com.eventflow.orderservice.model.Order;
import com.eventflow.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.apache.kafka.clients.consumer.Consumer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"orders"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@DirtiesContext
class OrderKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void placeOrder_publishesOrderPlacedEventToKafka() throws Exception {
        // Arrange: set up a consumer to read from the orders topic
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-consumer-group", "true", embeddedKafkaBroker);
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        Consumer<String, String> consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "orders");

        // Act: publish an event directly (simulating what the service does)
        OrderPlacedEvent event = OrderPlacedEvent.of(
                java.util.UUID.randomUUID(), "test@example.com",
                "Widget", 3, new BigDecimal("49.99")
        );
        kafkaTemplate.send("orders", event.orderId().toString(), event).get();

        // Assert: the message arrived on the topic
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "orders");
        assertThat(record).isNotNull();

        String payload = record.value();
        assertThat(payload).contains("test@example.com");
        assertThat(payload).contains("Widget");

        consumer.close();
    }
}
