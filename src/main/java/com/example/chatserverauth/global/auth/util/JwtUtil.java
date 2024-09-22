package com.example.chatserverauth.global.auth.util;

import com.example.chatserverauth.domain.dto.UserInfoDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j(topic = "JWT UTIL")
public class JwtUtil {
    private final String ROLE_KEY = "auth";
    private final String BEARER_PREFIX = "Bearer ";
    private final SecretKey secretKey;

    private final long ACCESS_TOKEN_EAT = 120 * 30 * 1000L; // 1H
    private final long REFRESH_TOKEN_EAT = 7 * 24 * 60 * 60 * 1000L; // 7D

    public JwtUtil(@Value("${jwt.secret.key}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String createToken(TokenPayload payload) {
        return BEARER_PREFIX +
                Jwts.builder()
                        .subject(payload.getSub()) // 사용자 식별자값(ID)
                        .claim(ROLE_KEY, payload.getRole()) // 사용자 권한
                        .expiration(payload.getExpiresAt()) // 만료 시간
                        .issuedAt(payload.getIat()) // 발급일
                        .id(payload.getJti()) // JWT ID
                        .signWith(secretKey) // 암호화 Key & 알고리즘
                        .compact();
    }

    // 토큰이 만료되었는지 확인하는 메서드
    public boolean isTokenExpired(String token) {
        return this.getClaims(token).getExpiration().before(new Date());
    }

    // 토큰으로부터 username(여기서는 이메일) 추출하면서 동시에 토큰 파싱 검증
    public String getUsernameFromToken(String token) {
        return this.getClaims(token).getSubject();
    }

    // 토큰으로부터 UserInfo(username(email), user role)을 반환하는 메소드
    public UserInfoDTO getUserInfoFromToken(String tokenValue, String id) {
        String token = extractToken(tokenValue);
        Claims claims = getClaims(token);

        String username = claims.getSubject();
        String role = claims.get(ROLE_KEY, String.class);

        tokenValue = URLEncoder.encode(tokenValue, StandardCharsets.UTF_8);
        tokenValue = tokenValue.replace("+", "%20");
        log.info("여전히 유효한 토큰: {}", tokenValue);

        return new UserInfoDTO(id, username, role, tokenValue);
    }

    // 만료만 된 토큰으로부터 UserInfo(username(email), user role)을 반환하는 메소드
    public UserInfoDTO getUserInfoFromExpiredToken(ExpiredJwtException exception, String id) {
        Claims claims = exception.getClaims();

        String username = claims.getSubject();
        String role = claims.get(ROLE_KEY, String.class);

        return new UserInfoDTO(id, username, role, "temp");
    }

    // 토큰이 있을 경우, 토큰 값을 추출하기
    public String extractToken(String tokenValue) {
        if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(BEARER_PREFIX)) {
            return tokenValue.substring(7);
        }

        throw new IllegalArgumentException("엑세스 토큰이 확인되지 않습니다.");
    }

    // 토큰의 만료일자 파싱(정상적인 토큰)
    public Date getTokenIat(String token) {
        return this.getClaims(token).getIssuedAt();
    }

    // 클레임 꺼내기(토큰 검증)
    private Claims getClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 만료만 된 엑세스 토큰 재발급용
    public String getUsernameFromExpiredJwt(ExpiredJwtException exception) {
        return exception.getClaims().getSubject();
    }

    // 새로운 엑세스 토큰 생성
    public String createNewAccessToken(UserInfoDTO userInfoDTO) {
        String email = userInfoDTO.getEmail();
        String role = userInfoDTO.getRole();
        Date date = new Date();

        return createToken(createTokenPayload(email, date, ACCESS_TOKEN_EAT, role));
    }

    // 토큰 페이로드 생성
    private TokenPayload createTokenPayload(String email, Date date, long seconds, String role){
        return new TokenPayload(
                email, UUID.randomUUID().toString(), date, new Date(date.getTime()+seconds), role
        );
    }
}