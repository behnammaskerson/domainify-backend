package com.domainify.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * CAPTCHA configuration options.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptchaSettings {

    public enum Difficulty {
        EASY,    // 4 characters, simple fonts
        MEDIUM,  // 5 characters, some distortion
        HARD     // 6 characters, heavy distortion
    }

    public enum CharacterType {
        DIGITS,      // 0-9 only
        LETTERS,     // A-Z only (no confusing chars like O,0,I,1)
        MIXED        // Both digits and letters
    }

    private boolean enabled = false;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private CharacterType characterType = CharacterType.MIXED;
    private int width = 200;
    private int height = 60;
    private int expiryMinutes = 10;
    private boolean enableNoise = true;
    private boolean enableDistortion = true;

    public CaptchaSettings() {
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public CharacterType getCharacterType() {
        return characterType;
    }

    public void setCharacterType(CharacterType characterType) {
        this.characterType = characterType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(int expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }

    public boolean isEnableNoise() {
        return enableNoise;
    }

    public void setEnableNoise(boolean enableNoise) {
        this.enableNoise = enableNoise;
    }

    public boolean isEnableDistortion() {
        return enableDistortion;
    }

    public void setEnableDistortion(boolean enableDistortion) {
        this.enableDistortion = enableDistortion;
    }

    public int getCharacterCount() {
        return switch (difficulty) {
            case EASY -> 4;
            case MEDIUM -> 5;
            case HARD -> 6;
        };
    }

    public String getCharacterPool() {
        return switch (characterType) {
            case DIGITS -> "23456789"; // Exclude 0,1 to avoid confusion
            case LETTERS -> "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Exclude I,O to avoid confusion with 1,0
            case MIXED -> "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        };
    }
}