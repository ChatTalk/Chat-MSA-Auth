package com.example.chatserverauth.global.config;

import com.example.chatserverauth.domain.dto.TokenDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// API Gateway 에서 날 것의 엑세스 토큰(String)을 전달받기 위한 카프카 컨슈머 설정
@EnableKafka
@Configuration
public class ReactiveKafkaConsumerConfig {

    @Value("${kafka.uri}")
    private String uri;

    @Value("${kafka.group-id}")
    private String groupId;

    @Value("${kafka.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${kafka.topic}")
    private String topic;

    @Bean
    public KafkaReceiver<String, TokenDTO> kafkaReceiver() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, uri);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TokenDTO.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        ReceiverOptions<String, TokenDTO> receiverOptions = ReceiverOptions.<String, TokenDTO>create(props)
                .subscription(Collections.singletonList(topic))
                .withValueDeserializer(new JsonDeserializer<>(TokenDTO.class, false))
                .commitInterval(Duration.ZERO) // 수동 커밋 사용
                .commitBatchSize(0); // 수동 커밋 크기 설정

        return KafkaReceiver.create(receiverOptions);
    }
}
