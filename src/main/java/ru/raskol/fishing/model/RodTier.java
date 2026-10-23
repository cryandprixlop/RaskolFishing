package ru.raskol.fishing.model;

/** Тиры удочек. VANILLA = обычная ванильная (без PDC-тега). */
public enum RodTier {
    VANILLA("§7Обычная удочка", "§7", null),
    AMATEUR("§eУдочка любителя", "§e", null),
    FISHERMAN("§bУдочка рыбака", "§b", "AMATEUR"),
    SEA_LUCK("§5Удочка морской удачи", "§5", "FISHERMAN");

    public final String displayName;
    public final String color;
    public final String previousTier;  // для какого тира этот является "следующим фрагментом"

    RodTier(String displayName, String color, String previousTier) {
        this.displayName = displayName;
        this.color = color;
        this.previousTier = previousTier;
    }

    public static RodTier fromName(String name) {
        if (name == null) return VANILLA;
        try {
            return RodTier.valueOf(name);
        } catch (IllegalArgumentException e) {
            return VANILLA;
        }
    }

    /** Какой фрагмент даёт этот тир (для следующего тира). */
    public RodTier droppedFragment() {
        for (RodTier t : values()) {
            if (name().equals(t.previousTier)) return t;
        }
        return null;
    }

    /** Какой фрагмент нужен для крафта этого тира. */
    public RodTier requiredFragment() {
        return fromName(previousTier);
    }
}
