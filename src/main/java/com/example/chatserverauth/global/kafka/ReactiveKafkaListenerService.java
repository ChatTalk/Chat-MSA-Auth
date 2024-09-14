package com.example.chatserverauth.global.kafka;

import com.example.chatserverauth.domain.dto.UserInfoDTO;
import com.example.chatserverauth.global.auth.service.JwtParseService;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveKafkaListenerService {

    private final JwtParseService jwtParseService;
    private final ReactiveKafkaProducerTemplate<String, UserInfoDTO> kafkaProducerTemplate;
//    private final ReactiveKafkaConsumerTemplate<String, String> kafkaConsumerTemplate;
    private final KafkaReceiver<String, String> kafkaReceiver;

    @PostConstruct
    public void startListening() {
        log.info("응답 시작");

        kafkaReceiver
                .receive()
                .flatMap(record -> {
                    log.info("받은 메시지: {}", record.value());

                    // JWT 파싱 로직 호출
                    return jwtParseService
                            .parseTokenWithCache(record.value())
                            .flatMap(userInfoDTO -> {
                                // 파싱된 UserInfoDTO 카프카로 송신
                                return kafkaProducerTemplate
                                        .send("auth", userInfoDTO)
                                        .then(Mono.just(record)); // 처리 완료 후, 현재 레코드 반환
                            })
                            .doOnSuccess(userInfoDTO -> log.info("JWT 파싱 완료: {}", userInfoDTO))
                            .doOnError(error -> log.error("토큰 처리 중 에러 발생: {}", error.getMessage()))
                            .then(Mono.just(record));
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(record -> {
                    // 메시지 처리 완료 후 오프셋 커밋
                    record.receiverOffset().commit().block();
                    log.info("메시지 처리 완료 및 커밋: {}", record.value());
                })
                .doOnError(error -> log.error("카프카 수신 중 에러 발생: {}", error.getMessage()))
                .subscribe();

//        kafkaConsumerTemplate
//                .receive()
//                .flatMap(record ->
//                        jwtParseService
//                                .parseTokenWithCache(record.value())
//                                .flatMap(userInfoDTO -> {
//                                    // 파싱된 UserInfoDTO 카프카로 송신
//                                    return kafkaProducerTemplate
//                                            .send("auth", userInfoDTO)
//                                            .then(Mono.just(record)); // 처리 완료 후, 현재 레코드 반환
//                                })
//                                .doOnError(
//                                        error -> {
//                                            log.error("토큰 처리 중 에러 발생: {}", error.getMessage());
//                                            throw new JwtException(error.getMessage());
//                                        }
//                                )
//                )
//                .doOnNext(record -> log.info("처리 완료된 메세지: {}", record.value()))
//                .doOnError(error -> {
//                            log.error("처리 실패 메세지: {}", error.getMessage());
//                            throw new RuntimeException(error.getMessage());
//                        }
//                )
//                .subscribe();
    }
}
