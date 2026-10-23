package ru.raskol.fishing.service;

import org.bukkit.entity.Player;
import ru.raskol.fishing.RaskolFishing;
import ru.raskol.fishing.hook.TownyHook;
import ru.raskol.fishing.hook.VaultHook;
import ru.raskol.fishing.model.License;
import ru.raskol.fishing.store.FishingStore;

public final class LicenseService {

    private final RaskolFishing plugin;
    private final FishingStore store;
    private final VaultHook vault;
    private final TownyHook towny;

    public LicenseService(RaskolFishing plugin, FishingStore store, VaultHook vault, TownyHook towny) {
        this.plugin = plugin;
        this.store = store;
        this.vault = vault;
        this.towny = towny;
    }

    /** Цена лицензии для игрока: цена его города или базовая. */
    public double priceFor(Player player) {
        double def = plugin.getConfig().getDouble("license.default-price", 500.0);
        if (!towny.isAvailable()) return def;
        String town = towny.residentTownName(player);
        if (town == null) return def;
        return store.townPrice(town, def);
    }

    /** true = лицензия активна. */
    public boolean hasValidLicense(Player player) {
        License lic = store.get(player.getUniqueId().toString());
        return lic != null && lic.isValid();
    }

    /** Покупка/продление лицензии. */
    public BuyResult buy(Player player) {
        String town = towny.residentTownName(player);
        if (town == null) {
            return new BuyResult(false, "§cЛицензия выдаётся только жителям городов. Вступите в город.");
        }
        double price = priceFor(player);
        int days = plugin.getConfig().getInt("license.duration-days", 30);

        if (!vault.isAvailable()) {
            return new BuyResult(false, "§cЭкономика недоступна.");
        }
        if (!vault.has(player, price)) {
            return new BuyResult(false, "§cНедостаточно средств: нужно §b" + vault.format(price)
                    + "§c, у тебя §b" + vault.format(vault.balance(player)) + "§c.");
        }
        if (!vault.withdraw(player, price)) {
            return new BuyResult(false, "§cНе удалось списать деньги.");
        }

        // Деньги в казну города игрока
        String via = null;
        if (towny.depositTownBank(town, price, "RaskolFishing: лицензия " + player.getName())) {
            via = "банк Towny города " + town;
        } else {
            String account = plugin.getConfig().getString("town-account-prefix", "town-") + town.toLowerCase();
            vault.createAccount(account);
            if (vault.depositAccount(account, price)) via = "аккаунт " + account;
        }

        if (via == null) {
            vault.deposit(player, price);
            return new BuyResult(false, "§cКазна города недоступна — операция отменена, деньги возвращены.");
        }

        store.issue(player.getUniqueId().toString(), player.getName(), days);
        return new BuyResult(true, "§a✔ Лицензия рыбака получена на §e" + days
                + " дн.§a. Заплачено §b" + vault.format(price) + "§a → " + via + ".");
    }

    public static final class BuyResult {
        public final boolean success;
        public final String message;
        public BuyResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
