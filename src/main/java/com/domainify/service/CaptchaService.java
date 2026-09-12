package com.domainify.service;

import com.domainify.entity.CaptchaSettings;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private final TicketSettingsService ticketSettingsService;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    // In-memory storage for CAPTCHA tokens (in production, use Redis or database)
    private final Map<String, CaptchaToken> activeCaptchas = new ConcurrentHashMap<>();

    public CaptchaService(TicketSettingsService ticketSettingsService, ObjectMapper objectMapper) {
        this.ticketSettingsService = ticketSettingsService;
        this.objectMapper = objectMapper;
    }

    public static class CaptchaResponse {
        private final String token;
        private final String imageDataUrl;

        public CaptchaResponse(String token, String imageDataUrl) {
            this.token = token;
            this.imageDataUrl = imageDataUrl;
        }

        public String getToken() {
            return token;
        }

        public String getImageDataUrl() {
            return imageDataUrl;
        }
    }

    private static class CaptchaToken {
        private final String answer;
        private final Instant expiresAt;
        private final String clientIp;

        public CaptchaToken(String answer, Instant expiresAt, String clientIp) {
            this.answer = answer;
            this.expiresAt = expiresAt;
            this.clientIp = clientIp;
        }

        public String getAnswer() {
            return answer;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public String getClientIp() {
            return clientIp;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Generate a new CAPTCHA challenge.
     */
    public CaptchaResponse generateCaptcha(HttpServletRequest request) {
        CaptchaSettings settings = getCaptchaSettings();
        
        if (!settings.isEnabled()) {
            throw new ApiException(ErrorCode.CAPTCHA_DISABLED);
        }

        // Clean up expired tokens
        cleanupExpiredTokens();

        String answer = generateRandomString(settings);
        String token = generateToken();
        String clientIp = getClientIp(request);
        
        Instant expiresAt = Instant.now().plusSeconds(settings.getExpiryMinutes() * 60L);
        activeCaptchas.put(token, new CaptchaToken(answer, expiresAt, clientIp));

        String imageDataUrl = generateCaptchaImage(answer, settings);
        
        return new CaptchaResponse(token, imageDataUrl);
    }

    /**
     * Validate a CAPTCHA response.
     */
    public boolean validateCaptcha(String token, String userAnswer, HttpServletRequest request) {
        CaptchaSettings settings = getCaptchaSettings();
        
        if (!settings.isEnabled()) {
            return true; // If CAPTCHA is disabled, always pass validation
        }

        if (!StringUtils.hasText(token) || !StringUtils.hasText(userAnswer)) {
            return false;
        }

        CaptchaToken captchaToken = activeCaptchas.remove(token); // Remove token after use
        if (captchaToken == null) {
            return false; // Token not found or already used
        }

        if (captchaToken.isExpired()) {
            return false;
        }

        String clientIp = getClientIp(request);
        if (!clientIp.equals(captchaToken.getClientIp())) {
            log.warn("CAPTCHA IP mismatch: token created from {}, validation from {}", captchaToken.getClientIp(), clientIp);
            return false;
        }

        return userAnswer.equalsIgnoreCase(captchaToken.getAnswer());
    }

    /**
     * Check if CAPTCHA is enabled in settings.
     */
    public boolean isCaptchaEnabled() {
        return getCaptchaSettings().isEnabled();
    }

    /**
     * Get current CAPTCHA settings, with defaults if not configured.
     */
    public CaptchaSettings getCaptchaSettings() {
        String json = ticketSettingsService.getOrCreate().getCaptchaSettingsJson();
        if (!StringUtils.hasText(json)) {
            return new CaptchaSettings(); // Return defaults
        }
        
        try {
            return objectMapper.readValue(json, CaptchaSettings.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse CAPTCHA settings JSON: {}", e.getMessage());
            return new CaptchaSettings(); // Return defaults on parse error
        }
    }

    private String generateRandomString(CaptchaSettings settings) {
        String pool = settings.getCharacterPool();
        int length = settings.getCharacterCount();
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            sb.append(pool.charAt(random.nextInt(pool.length())));
        }
        
        return sb.toString();
    }

    private String generateToken() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCaptchaImage(String text, CaptchaSettings settings) {
        BufferedImage image = new BufferedImage(settings.getWidth(), settings.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        try {
            // Set rendering hints for better quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, settings.getWidth(), settings.getHeight());
            
            // Add noise if enabled
            if (settings.isEnableNoise()) {
                addNoise(g2d, settings);
            }
            
            // Draw text
            drawText(g2d, text, settings);
            
            // Convert to base64 data URL
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            
            return "data:image/png;base64," + base64;
            
        } catch (IOException e) {
            log.error("Failed to generate CAPTCHA image", e);
            throw new RuntimeException("CAPTCHA image generation failed", e);
        } finally {
            g2d.dispose();
        }
    }

    private void addNoise(Graphics2D g2d, CaptchaSettings settings) {
        g2d.setColor(new Color(200, 200, 200));
        
        // Add random lines
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(settings.getWidth());
            int y1 = random.nextInt(settings.getHeight());
            int x2 = random.nextInt(settings.getWidth());
            int y2 = random.nextInt(settings.getHeight());
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Add random dots
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(settings.getWidth());
            int y = random.nextInt(settings.getHeight());
            g2d.fillOval(x, y, 2, 2);
        }
    }

    private void drawText(Graphics2D g2d, String text, CaptchaSettings settings) {
        int fontSize = Math.max(24, settings.getHeight() / 3);
        Font baseFont = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
        
        int charWidth = settings.getWidth() / text.length();
        int startX = charWidth / 4;
        
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            
            // Random color (dark colors for better contrast)
            Color color = new Color(
                random.nextInt(100),
                random.nextInt(100),
                random.nextInt(100)
            );
            g2d.setColor(color);
            
            // Font variation
            int fontSizeVariation = fontSize + random.nextInt(8) - 4;
            Font font = baseFont.deriveFont((float) fontSizeVariation);
            g2d.setFont(font);
            
            // Position with some randomness
            int x = startX + i * charWidth + random.nextInt(10) - 5;
            int y = settings.getHeight() / 2 + fontSize / 3 + random.nextInt(10) - 5;
            
            // Rotation if distortion is enabled
            if (settings.isEnableDistortion() && settings.getDifficulty() != CaptchaSettings.Difficulty.EASY) {
                double angle = (random.nextDouble() - 0.5) * 0.5; // -0.25 to 0.25 radians
                AffineTransform transform = g2d.getTransform();
                g2d.rotate(angle, x, y);
                g2d.drawString(String.valueOf(ch), x, y);
                g2d.setTransform(transform);
            } else {
                g2d.drawString(String.valueOf(ch), x, y);
            }
        }
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        activeCaptchas.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }
        
        return request.getRemoteAddr();
    }
}