//package com.example.chatserverauth.global.kafka;
//
//import com.example.chatserverauth.domain.dto.TokenDTO;
//import com.example.chatserverauth.domain.dto.UserInfoDTO;
//import com.example.chatserverauth.global.auth.service.JwtParseService;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//import reactor.kafka.receiver.KafkaReceiver;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ReactiveKafkaListenerService {
//
//    private final JwtParseService jwtParseService;
//    private final ReactiveKafkaProducerTemplate<String, UserInfoDTO> kafkaProducerTemplate;
//    private final KafkaReceiver<String, TokenDTO> kafkaReceiver;
//
//    @PostConstruct
//    public void startListening() {
//        log.info("응답 시작");
//
//        kafkaReceiver
//                .receive()
//                .flatMap(record -> {
//                    log.info("받은 메시지 토큰: {}", record.value().getToken());
//
//                    // JWT 파싱 로직 호출
//                    return jwtParseService
//                            .parseTokenWithCache(record.value())
//                            .flatMap(userInfoDTO -> {
//                                // 파싱된 UserInfoDTO 카프카로 송신
//                                return kafkaProducerTemplate
//                                        // 동일한 파티션을 왕복
//                                        .send("auth", record.value().getId().toString(), userInfoDTO)
//                                        .then(Mono.just(record)); // 처리 완료 후, 현재 레코드 반환
//                            })
//                            .doOnSuccess(userInfoDTO -> log.info("JWT 파싱 완료: {}", userInfoDTO))
//                            .doOnError(error -> log.error("토큰 처리 중 에러 발생: {}", error.getMessage()))
//                            .then(Mono.just(record));
//                })
//                .doOnError(error -> log.error("카프카 수신 중 에러 발생: {}", error.getMessage()))
//                .subscribe();
//    }
//}
