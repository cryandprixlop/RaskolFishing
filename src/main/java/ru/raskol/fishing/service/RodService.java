package ru.raskol.fishing.service;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.raskol.fishing.RaskolFishing;
import ru.raskol.fishing.model.RodTier;

import java.util.ArrayList;
import java.util.List;

public final class RodService {

    private final RaskolFishing plugin;
    public final NamespacedKey rodTierKey;
    public final NamespacedKey rodFragmentKey;
    public final NamespacedKey goldenFishKey;

    public RodService(RaskolFishing plugin) {
        this.plugin = plugin;
        this.rodTierKey = new NamespacedKey(plugin, "rod_tier");
        this.rodFragmentKey = new NamespacedKey(plugin, "rod_fragment");
        this.goldenFishKey = new NamespacedKey(plugin, "golden_fish");
    }

    public RodTier tierOf(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return RodTier.VANILLA;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String tag = pdc.get(rodTierKey, PersistentDataType.STRING);
        if (tag == null) return RodTier.VANILLA;
        try { return RodTier.valueOf(tag); }
        catch (IllegalArgumentException e) { return RodTier.VANILLA; }
    }

    public ItemStack createRod(RodTier tier) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(tier.displayName);
        List<String> lore = new ArrayList<>();
        lore.add(tier.color + "Удочка уровня " + tier.name());
        if (tier == RodTier.SEA_LUCK) {
            lore.add("§7Редчайший инструмент рыбака");
        }
        meta.setLore(lore);
        if (tier != RodTier.VANILLA) {
            meta.getPersistentDataContainer().set(rodTierKey, PersistentDataType.STRING, tier.name());
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createFragment(RodTier tier, int amount) {
        if (tier == RodTier.VANILLA) return null;
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(tier.color + "Фрагмент " + tier.displayName);
        List<String> lore = new ArrayList<>();
        lore.add("§7Нужно §e3 §7фрагмента для крафта");
        lore.add("§7" + tier.displayName);
        lore.add("§8Шанс успеха: 25%");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(rodFragmentKey, PersistentDataType.STRING, tier.name());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createGoldenFish(int amount) {
        ItemStack item = new ItemStack(Material.TROPICAL_FISH, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§6✦ Золотая рыбка ✦");
        List<String> lore = new ArrayList<>();
        lore.add("§eРедчайший улов");
        lore.add("§7Используется в легендарных крафтах");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(goldenFishKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public RodTier fragmentTierOf(ItemStack item) {
        if (item == null || item.getType() != Material.PRISMARINE_SHARD) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String tag = meta.getPersistentDataContainer().get(rodFragmentKey, PersistentDataType.STRING);
        if (tag == null) return null;
        try { return RodTier.valueOf(tag); }
        catch (IllegalArgumentException e) { return null; }
    }

    public boolean isGoldenFish(ItemStack item) {
        if (item == null || item.getType() != Material.TROPICAL_FISH) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(goldenFishKey, PersistentDataType.BYTE);
    }

    public String stripColor(String text) {
        return ChatColor.stripColor(text);
    }
}
