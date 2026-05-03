package com.softcraft.autocourier;

public class PasswordStrengthUtil {
    public static int calculateStrength(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;
        // Normalize to 0-100
        return Math.min(100, score * 20);
    }

    public static String getStrengthText(int strength) {
        if (strength <= 20) return "Very Weak";
        if (strength <= 40) return "Weak";
        if (strength <= 60) return "Medium";
        if (strength <= 80) return "Strong";
        return "Very Strong";
    }

    public static int getStrengthColor(int strength) {
        if (strength <= 20) return android.graphics.Color.parseColor("#F44336"); // Red
        if (strength <= 40) return android.graphics.Color.parseColor("#FF9800"); // Orange
        if (strength <= 60) return android.graphics.Color.parseColor("#FFC107"); // Amber
        if (strength <= 80) return android.graphics.Color.parseColor("#8BC34A"); // Light Green
        return android.graphics.Color.parseColor("#4CAF50"); // Green
    }
}