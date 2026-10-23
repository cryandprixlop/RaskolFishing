package ru.raskol.fishing.listener;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import ru.raskol.fishing.RaskolFishing;
import ru.raskol.fishing.model.RodTier;
import ru.raskol.fishing.service.LicenseService;
import ru.raskol.fishing.service.RodService;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class FishingListener implements Listener {

    private static final Material[] FISHES = {
            Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH
    };

    private final RaskolFishing plugin;
    private final LicenseService licenseService;
    private final RodService rodService;
    private final Random random = new Random();
    private final Map<UUID, Long> lastWarn = new HashMap<>();

    public FishingListener(RaskolFishing plugin, LicenseService licenseService, RodService rodService) {
        this.plugin = plugin;
        this.licenseService = licenseService;
        this.rodService = rodService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("fishing.use")) return;

        // Проверка лицензии
        if (!licenseService.hasValidLicense(player)) {
            event.setCancelled(true);
            warn(player, "§cДля рыбалки нужна лицензия! §e/fish license buy");
            return;
        }

        if (!(event.getCaught() instanceof Item item)) return;
        ItemStack drop = item.getItemStack();

        // Определяем тир удочки
        ItemStack rod = player.getInventory().getItemInMainHand();
        RodTier tier = rodService.tierOf(rod);
        if (tier == null) tier = RodTier.VANILLA;

        // Отменяем ванильный дроп (книги, сапоги и т.п.)
        event.setCancelled(true);
        item.remove();

        // Рулетка: золотая → фрагмент → множитель → обычная рыба
        double roll = random.nextDouble(100.0);
        double acc = 0.0;

        // 1) Золотая рыба
        double golden = getTierConfig(tier, "golden-chance", 0.0);
        acc += golden;
        if (roll < acc) {
            dropInventories(player, rodService.createGoldenFish(1));
            player.sendMessage("§6✦ Редчайший улов: §eЗолотая рыбка§6!");
            plugin.getLogger().info("[Fishing] " + player.getName() + " поймал золотую рыбку (" + tier + ")");
            return;
        }

        // 2) Фрагмент следующей удочки
        double fragChance = getTierConfig(tier, "next-fragment-chance", 0.0);
        acc += fragChance;
        if (roll < acc) {
            RodTier fragTier = tier.droppedFragment();
            if (fragTier != null) {
                dropInventories(player, rodService.createFragment(fragTier, 1));
                player.sendMessage("§7⚙ Улов: " + fragTier.displayName + " §7(фрагмент)");
                plugin.getLogger().info("[Fishing] " + player.getName() + " поймал фрагмент " + fragTier);
                return;
            }
        }

        // 3) Множитель улова
        int multiplier = 1;
        double doubleC = getTierConfig(tier, "double-chance", 0.0);
        double tripleC = getTierConfig(tier, "triple-chance", 0.0);
        double quadC = getTierConfig(tier, "quad-chance", 0.0);
        double mRoll = random.nextDouble(100.0);
        if (mRoll < quadC) multiplier = 4;
        else if (mRoll < quadC + tripleC) multiplier = 3;
        else if (mRoll < quadC + tripleC + doubleC) multiplier = 2;

        // 4) Обычная рыба × множитель
        Material fishType = FISHES[random.nextInt(FISHES.length)];
        ItemStack fish = new ItemStack(fishType, multiplier);
        dropInventories(player, fish);

        if (multiplier > 1) {
            player.sendMessage("§e✦ Удачный улов! §7×" + multiplier + " " + fishName(fishType));
        }
    }

    private double getTierConfig(RodTier tier, String key, double def) {
        String section = switch (tier) {
            case VANILLA -> "vanilla";
            case AMATEUR -> "amateur";
            case FISHERMAN -> "fisherman";
            case SEA_LUCK -> "sea_luck";
        };
        return plugin.getConfig().getDouble("loot." + section + "." + key, def);
    }

    private void dropInventories(Player player, ItemStack item) {
        if (item == null) return;
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
    }

    private String fishName(Material m) {
        return switch (m) {
            case COD -> "треска";
            case SALMON -> "лосось";
            case TROPICAL_FISH -> "тропическая рыба";
            case PUFFERFISH -> "иглобрюх";
            default -> "рыба";
        };
    }

    private void warn(Player player, String message) {
        long now = System.currentTimeMillis();
        Long prev = lastWarn.get(player.getUniqueId());
        if (prev != null && now - prev < 3000) return;
        lastWarn.put(player.getUniqueId(), now);
        player.sendMessage(message);
    }
}
