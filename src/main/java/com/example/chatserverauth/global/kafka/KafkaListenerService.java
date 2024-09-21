package com.example.chatserverauth.global.kafka;

import com.example.chatserverauth.domain.dto.TokenDTO;
import com.example.chatserverauth.domain.dto.UserInfoDTO;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaListenerService {

    @Value("${kafka.topic}")
    private String topic;

    @Value("${kafka.group-id}")
    private String groupId;

    private final KafkaTemplate<String, UserInfoDTO> kafkaTemplate;

    @KafkaListener(topics = "result", groupId = "auth")
    public void listen(TokenDTO tokenDTO) {
        log.info("수신 토큰: {}", tokenDTO.getToken());
        if (tokenDTO.getToken() == null) throw new JwtException("엑세스 토큰이 존재하지 않습니다.");
    }
}
