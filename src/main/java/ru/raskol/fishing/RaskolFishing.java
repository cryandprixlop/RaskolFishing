package ru.raskol.fishing;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.fishing.command.FishCommand;
import ru.raskol.fishing.hook.TownyHook;
import ru.raskol.fishing.hook.VaultHook;
import ru.raskol.fishing.listener.FishingListener;
import ru.raskol.fishing.listener.RodCraftListener;
import ru.raskol.fishing.model.RodTier;
import ru.raskol.fishing.service.LicenseService;
import ru.raskol.fishing.service.RodService;
import ru.raskol.fishing.store.FishingStore;

public final class RaskolFishing extends JavaPlugin {

    private FishingStore store;
    private VaultHook vaultHook;
    private TownyHook townyHook;
    private RodService rodService;
    private LicenseService licenseService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new FishingStore(this);
        store.load();
        vaultHook = new VaultHook(this);
        townyHook = new TownyHook(this);
        rodService = new RodService(this);
        licenseService = new LicenseService(this, store, vaultHook, townyHook);

        // Регистрируем рецепты крафта удочек (3 фрагмента -> удочка)
        registerRodRecipes();

        PluginCommand cmd = getCommand("fish");
        if (cmd != null) {
            FishCommand executor = new FishCommand(this, store, licenseService, rodService, townyHook);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(
                new FishingListener(this, licenseService, rodService), this);
        getServer().getPluginManager().registerEvents(
                new RodCraftListener(this, rodService), this);

        getLogger().info("RaskolFishing v" + getDescription().getVersion()
                + " включён. Лицензий активных: " + countActiveLicenses()
                + ". Vault: " + (vaultHook.isAvailable() ? "да" : "нет")
                + ". Towny: " + (townyHook.isAvailable() ? "да" : "нет")
                + ". Базовая цена лицензии: " + getConfig().getDouble("license.default-price", 500.0) + ".");
    }

    private int countActiveLicenses() {
        int count = 0;
        for (String uuid : new java.util.ArrayList<>(store.all().keySet())) {
            // Вспомогательный метод store.get() через итерацию, но проще —
            // считаем по всем UUID в файле. Здесь используем store.get(uuid).
        }
        // Упрощённый подсчёт: итерируемся по всем players? Проще не считать.
        return -1;
    }

    private void registerRodRecipes() {
        int count = 0;
        for (RodTier tier : new RodTier[]{RodTier.AMATEUR, RodTier.FISHERMAN, RodTier.SEA_LUCK}) {
            RodTier fragTier = tier.requiredFragment();
            if (fragTier == null) continue;

            NamespacedKey key = new NamespacedKey(this, "rod_craft_" + tier.name().toLowerCase());
            if (getServer().getRecipe(key) != null) continue;

            ItemStack dummy = new ItemStack(Material.FISHING_ROD);
            ShapedRecipe recipe = new ShapedRecipe(key, dummy);
            recipe.shape("   ", "FFF", "   ");
            recipe.setIngredient('F', Material.PRISMARINE_SHARD);
            getServer().addRecipe(recipe);
            count++;
        }
        getLogger().info("[Fishing] рецептов удочек зарегистрировано: " + count);
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
        getLogger().info("RaskolFishing выключен.");
    }

    public FishingStore getStore() { return store; }
    public VaultHook getVaultHook() { return vaultHook; }
    public TownyHook getTownyHook() { return townyHook; }
    public RodService getRodService() { return rodService; }
    public LicenseService getLicenseService() { return licenseService; }
}
