package com.example.chatserverauth.global.auth.service;

import com.example.chatserverauth.domain.dto.TokenDTO;
import com.example.chatserverauth.domain.dto.UserInfoDTO;
import com.example.chatserverauth.global.auth.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.chatserverauth.global.constant.Constants.REDIS_ACCESS_KEY;

/**
 * 엑세스 토큰 기반 캐시 키 : 디바이스 핑거프린트 적용 예정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtParseService {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, UserInfoDTO> userInfoTemplate;

    // 동기적으로 토큰을 파싱하거나 캐시에서 조회하는 로직
    public UserInfoDTO parseTokenWithCache(TokenDTO tokenDTO) {
        String tokenValue = tokenDTO.getToken();
        String token = URLDecoder.decode(tokenValue, StandardCharsets.UTF_8);

        log.info("딱 받은 시점의 토큰값: {}", tokenValue);

        // 캐시 미스 시 JWT 파싱 후 캐시에 저장
        return parseAndCacheToken(token, tokenDTO.getId());
    }

    // 날 것의 토큰이 입력됐다
    private UserInfoDTO parseAndCacheToken(String token, String id) {
        try {
            // JWT 토큰 파싱
            return jwtUtil.getUserInfoFromToken(token, id);
        } catch (ExpiredJwtException ex) {
            // 만료된 토큰 처리
            UserInfoDTO userInfoDTO = jwtUtil.getUserInfoFromExpiredToken(ex, id);
            log.info("만료 이메일: {}", userInfoDTO.getEmail());
            log.info("만료 역할: {}", userInfoDTO.getRole());
            String newAccessToken = URLEncoder.encode(jwtUtil.createNewAccessToken(userInfoDTO), StandardCharsets.UTF_8);
            newAccessToken = newAccessToken.replace("+", "%20");
            log.info("새롭게 생성한 토큰: {}", newAccessToken);
            userInfoDTO.setToken(newAccessToken);

            // Redis에 새 토큰 저장(무효화 예정 id + 새로운 데이터)
            userInfoTemplate.opsForValue().set(REDIS_ACCESS_KEY + id, userInfoDTO, 120 * 30, TimeUnit.SECONDS);
            return userInfoDTO;
        } catch (JwtException ex) {
            log.error("만료 외에 다른 토큰 예외 발생: {}", ex.getMessage());
            throw new JwtException(ex.getMessage());
        }
    }
}
