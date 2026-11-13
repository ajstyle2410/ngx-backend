package com.npx.npx.security;

import com.npx.npx.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
	private final SecretKey key;
	private final long expiryMinutes;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiry-minutes}") long expiryMinutes) {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET must contain at least 32 characters.");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expiryMinutes = expiryMinutes;
	}

	public String createToken(AppUser user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("role", user.getRole())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(expiryMinutes * 60)))
				.signWith(key)
				.compact();
	}

	public String subject(String token) {
		return claims(token).getSubject();
	}

	private Claims claims(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}
}
