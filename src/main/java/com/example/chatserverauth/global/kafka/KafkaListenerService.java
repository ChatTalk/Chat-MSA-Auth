package com.example.chatserverauth.global.kafka;

import com.example.chatserverauth.domain.dto.TokenDTO;
import com.example.chatserverauth.domain.dto.UserInfoDTO;
import com.example.chatserverauth.global.auth.service.JwtParseService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.example.chatserverauth.global.constant.Constants.REDIS_ACCESS_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaListenerService {

    @Value("${kafka.topic}")
    private String topic;

    @Value("${kafka.group-id}")
    private String groupId;

    private final RedisTemplate<String, UserInfoDTO> userInfoTemplate;
    private final KafkaTemplate<String, UserInfoDTO> kafkaTemplate;
    private final JwtParseService jwtParseService;

    @KafkaListener(topics = "result", groupId = "auth")
    public void listen(TokenDTO tokenDTO) {
        log.info("수신 토큰: {}", tokenDTO.getToken());
        if (tokenDTO.getToken() == null) throw new JwtException("엑세스 토큰이 존재하지 않습니다.");

        UserInfoDTO userInfoDTO = jwtParseService.parseTokenWithCache(tokenDTO);
        log.info("송신 파티션 키: {}\n송신 파싱 이메일: {}", tokenDTO.getId(), userInfoDTO.getEmail());

        userInfoTemplate.opsForValue().set(REDIS_ACCESS_KEY + tokenDTO.getId(), userInfoDTO, Duration.ofHours(1));

        log.info("로직 수행 시간: {}", System.nanoTime());
    }
}
