package cn.lut.imserver.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * JWT工具类，用于生成和验证JWT token
 */
@Component
public class JWTUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Autowired
    private RedisUtil redisUtil;

    private final Logger logger = Logger.getLogger(JWTUtil.class.getName());

    private SecretKey key;

    @PostConstruct
    private void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 创建JWT token
    public String createToken(String uid, String username) {
        String token = Jwts.builder()
                .claim("username", username)
                .claim("uid", uid)
                .expiration(new java.util.Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        redisUtil.setUserToken(String.valueOf(uid), token, expiration);
        return token;
    }

    // 验证并解析JWT token
    public Jws<Claims> parseToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            // 检查token是否在Redis中存在
            String uid = String.valueOf(jws.getPayload().get("uid"));
            if (!redisUtil.checkTokenValid(uid, token)) {
                return null;
            }

            return jws;
        } catch (Exception e) {
            logger.warning("When parse Token get exception: " + e.getMessage());
            return null;
        }
    }
}
