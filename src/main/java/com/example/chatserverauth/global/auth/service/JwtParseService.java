package com.example.chatserverauth.global.auth.service;

import com.example.chatserverauth.domain.dto.UserInfoDTO;
import com.example.chatserverauth.global.auth.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.example.chatserverauth.global.constant.Constants.REDIS_ACCESS_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtParseService {

    private final JwtUtil jwtUtil;
    private final ReactiveRedisTemplate<String, UserInfoDTO> userInfoTemplate;
    private final KafkaTemplate<String, UserInfoDTO> kafkaTemplate;

    // 토큰을 파싱하거나 캐시에서 조회하는 로직
    public Mono<UserInfoDTO> parseTokenWithCache(String token) {
        // 먼저 Redis 캐시에서 조회
        return userInfoTemplate
                .opsForValue()
                .get(REDIS_ACCESS_KEY + token)
                .switchIfEmpty(parseAndCacheToken(token)); // 캐시 미스 시 JWT 파싱 후 캐시에 저장
    }

    // 날 것의 토큰이 입력됐다
    private Mono<UserInfoDTO> parseAndCacheToken(String token) {
        return Mono.defer(() -> {
            try {
                // JWT 토큰 파싱
                UserInfoDTO userInfo = jwtUtil.getUserInfoFromToken(token);
                return Mono.just(userInfo);
            } catch (ExpiredJwtException ex) {
                // 만료된 토큰 처리
                UserInfoDTO userInfoDTO = jwtUtil.getUserInfoFromExpiredToken(ex);
                String newAccessToken = jwtUtil.createNewAccessToken(userInfoDTO);

                // Redis에 새 토큰 저장
                return userInfoTemplate
                        .opsForValue()
                        .set(REDIS_ACCESS_KEY + newAccessToken, userInfoDTO, Duration.ofMillis(120 * 30 * 1000L))
                        .thenReturn(userInfoDTO);
            } catch (JwtException ex) {
                log.error("만료 외에 다른 토큰 예외 발생: {}", ex.getMessage());
                return Mono.error(new JwtException(ex.getMessage()));
            }
        });
    }

    // kafka 메시지 전송
    public Mono<Void> sendResponseToKafka(String topic, UserInfoDTO userInfoDTO) {
        return Mono.fromCallable(() -> kafkaTemplate.send(topic, userInfoDTO))
                .then();
    }
}
