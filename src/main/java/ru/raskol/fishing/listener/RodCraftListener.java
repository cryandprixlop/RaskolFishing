package ru.raskol.fishing.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import ru.raskol.fishing.RaskolFishing;
import ru.raskol.fishing.model.RodTier;
import ru.raskol.fishing.service.RodService;

import java.util.Random;

public final class RodCraftListener implements Listener {

    private final RaskolFishing plugin;
    private final RodService rodService;
    private final Random random = new Random();

    public RodCraftListener(RaskolFishing plugin, RodService rodService) {
        this.plugin = plugin;
        this.rodService = rodService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe sr)) return;
        if (!isOurRecipe(sr)) return;

        RodTier target = targetTier(sr);
        if (target == null) {
            event.getInventory().setResult(null);
            return;
        }
        if (!validateMatrix(event.getInventory(), target)) {
            event.getInventory().setResult(null);
            return;
        }
        event.getInventory().setResult(rodService.createRod(target));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe sr)) return;
        if (!isOurRecipe(sr)) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        RodTier target = targetTier(sr);
        if (target == null) return;
        if (!validateMatrix(event.getInventory(), target)) return;

        event.setCancelled(true);
        consumeFragments(event.getInventory(), target);

        double chance = plugin.getConfig().getDouble("rod.craft-success-chance", 25.0);
        boolean success = random.nextDouble(100.0) < chance;

        if (success) {
            ItemStack rod = rodService.createRod(target);
            player.getInventory().addItem(rod);
            player.sendMessage("§a✔ Крафт удался! Получена: " + target.displayName);
            plugin.getLogger().info("[Fishing] craft OK: " + player.getName() + " -> " + target);
        } else {
            player.sendMessage("§c✖ Крафт провалился: фрагменты сгорели в горне!");
            plugin.getLogger().info("[Fishing] craft FAIL: " + player.getName() + " -> " + target);
        }
    }

    private boolean isOurRecipe(ShapedRecipe sr) {
        return sr.getKey().getNamespace().equals(plugin.getName().toLowerCase())
                && sr.getKey().getKey().startsWith("rod_craft_");
    }

    private RodTier targetTier(ShapedRecipe sr) {
        String key = sr.getKey().getKey();
        if (!key.startsWith("rod_craft_")) return null;
        String name = key.substring("rod_craft_".length()).toUpperCase();
        try { return RodTier.valueOf(name); }
        catch (IllegalArgumentException e) { return null; }
    }

    private boolean validateMatrix(CraftingInventory inv, RodTier target) {
        ItemStack[] matrix = inv.getMatrix();
        if (matrix.length < 9) return false;

        RodTier fragTier = target.requiredFragment();
        if (fragTier == null) return false;

        int count = 0;
        for (ItemStack s : matrix) {
            if (s == null || s.getType() == Material.AIR) continue;
            if (rodService.fragmentTierOf(s) != fragTier) return false;
            count++;
        }
        return count >= 3;
    }

    private void consumeFragments(CraftingInventory inv, RodTier target) {
        ItemStack[] matrix = inv.getMatrix();
        int remaining = 3;
        for (int i = 0; i < matrix.length && remaining > 0; i++) {
            ItemStack s = matrix[i];
            if (s == null || s.getType() == Material.AIR) continue;
            if (rodService.fragmentTierOf(s) == null) continue;
            int take = Math.min(s.getAmount(), remaining);
            if (take >= s.getAmount()) {
                matrix[i] = null;
            } else {
                s.setAmount(s.getAmount() - take);
            }
            remaining -= take;
        }
        inv.setMatrix(matrix);
    }
}
