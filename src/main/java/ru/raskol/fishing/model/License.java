package ru.raskol.fishing.model;

public final class License {
    public String uuid;
    public String name;
    public long expiresAtEpochSeconds;

    public boolean isValid() {
        return expiresAtEpochSeconds > System.currentTimeMillis() / 1000L;
    }

    public long remainingSeconds() {
        long remain = expiresAtEpochSeconds - System.currentTimeMillis() / 1000L;
        return Math.max(0, remain);
    }

    public String remainingFormatted() {
        long s = remainingSeconds();
        long days = s / 86400;
        long hours = (s % 86400) / 3600;
        long mins = (s % 3600) / 60;
        if (days > 0) return days + " дн. " + hours + " ч";
        if (hours > 0) return hours + " ч " + mins + " мин";
        return mins + " мин";
    }
}
