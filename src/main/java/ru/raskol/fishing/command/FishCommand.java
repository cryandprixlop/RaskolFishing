package ru.raskol.fishing.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.raskol.fishing.RaskolFishing;
import ru.raskol.fishing.hook.TownyHook;
import ru.raskol.fishing.model.License;
import ru.raskol.fishing.model.RodTier;
import ru.raskol.fishing.service.LicenseService;
import ru.raskol.fishing.service.RodService;
import ru.raskol.fishing.store.FishingStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FishCommand implements CommandExecutor, TabCompleter {

    private final RaskolFishing plugin;
    private final FishingStore store;
    private final LicenseService licenseService;
    private final RodService rodService;
    private final TownyHook towny;

    public FishCommand(RaskolFishing plugin, FishingStore store,
                       LicenseService licenseService, RodService rodService, TownyHook towny) {
        this.plugin = plugin;
        this.store = store;
        this.licenseService = licenseService;
        this.rodService = rodService;
        this.towny = towny;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "license" -> handleLicense(sender, args);
            case "rod" -> handleRod(sender, args);
            case "fragment" -> handleFragment(sender, args);
            case "golden" -> handleGolden(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== RaskolFishing ===");
        sender.sendMessage("§e/fish license buy §7— купить лицензию");
        sender.sendMessage("§e/fish license status §7— статус лицензии");
        sender.sendMessage("§e/fish license setprice <цена> §7— (король) сменить цену своего города");
        sender.sendMessage("§e/fish rod give <tier> <игрок> [кол-во] §7— выдать удочку");
        sender.sendMessage("§e/fish fragment give <tier> <игрок> [кол-во] §7— выдать фрагмент");
        sender.sendMessage("§e/fish golden give <игрок> [кол-во] §7— выдать золотую рыбку");
        sender.sendMessage("§e/fish reload §7— перезагрузить конфиг");
        sender.sendMessage("§7Тиры удочек: VANILLA, AMATEUR, FISHERMAN, SEA_LUCK");
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("fishing.admin")) return true;
        sender.sendMessage("§cНет прав.");
        return false;
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player p) return p;
        sender.sendMessage("§cТолько игроки.");
        return null;
    }

    /* ================= license ================= */

    private void handleLicense(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c/fish license <buy|status|setprice>");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "buy" -> {
                Player p = player(sender);
                if (p == null) return;
                LicenseService.BuyResult r = licenseService.buy(p);
                p.sendMessage(r.message);
            }
            case "status" -> {
                Player p = player(sender);
                if (p == null) return;
                License lic = store.get(p.getUniqueId().toString());
                if (lic == null || !lic.isValid()) {
                    p.sendMessage("§7Лицензия неактивна. Купи: §e/fish license buy");
                } else {
                    p.sendMessage("§aЛицензия активна. Осталось: §e" + lic.remainingFormatted());
                }
            }
            case "setprice" -> {
                if (!admin(sender)) return;
                Player p = player(sender);
                if (p == null) return;
                if (args.length < 3) { p.sendMessage("§c/fish license setprice <цена>"); return; }
                String town = towny.residentTownName(p);
                if (town == null && !p.hasPermission("fishing.admin")) {
                    p.sendMessage("§cТы должен быть жителем города.");
                    return;
                }
                if (!p.hasPermission("fishing.admin") && !towny.isKing(p)) {
                    p.sendMessage("§cТолько король может менять цену лицензии.");
                    return;
                }
                double price;
                try { price = Double.parseDouble(args[2]); }
                catch (NumberFormatException e) { p.sendMessage("§cЦена должна быть числом."); return; }
                if (price < 0) { p.sendMessage("§cЦена не может быть отрицательной."); return; }
                String target = (town != null) ? town : "DEFAULT";
                if (town != null) store.setTownPrice(town, price);
                else plugin.getConfig().set("license.default-price", price);
                p.sendMessage("§aЦена лицензии для " + target + " теперь §b" + price + "§a.");
            }
            default -> sender.sendMessage("§cНеизвестная подкоманда.");
        }
    }

    /* ================= rod / fragment / golden ================= */

    private void handleRod(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/fish rod give <VANILLA|AMATEUR|FISHERMAN|SEA_LUCK> <игрок> [кол-во]");
            return;
        }
        RodTier tier;
        try { tier = RodTier.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage("§cНеизвестный тир."); return; }
        Player target = plugin.getServer().getPlayer(args[3]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = args.length >= 5 ? parseInt(args[4], 1) : 1;
        for (int i = 0; i < amount; i++) target.getInventory().addItem(rodService.createRod(tier));
        sender.sendMessage("§aВыдано " + amount + " × " + tier.displayName + " игроку " + target.getName());
    }

    private void handleFragment(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/fish fragment give <AMATEUR|FISHERMAN|SEA_LUCK> <игрок> [кол-во]");
            return;
        }
        RodTier tier;
        try { tier = RodTier.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage("§cНеизвестный тир."); return; }
        if (tier == RodTier.VANILLA) { sender.sendMessage("§cУ обычной удочки нет фрагмента."); return; }
        Player target = plugin.getServer().getPlayer(args[3]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = args.length >= 5 ? parseInt(args[4], 1) : 1;
        target.getInventory().addItem(rodService.createFragment(tier, amount));
        sender.sendMessage("§aВыдано " + amount + " фрагментов " + tier.displayName + " игроку " + target.getName());
    }

    private void handleGolden(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 3 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§c/fish golden give <игрок> [кол-во]");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;
        target.getInventory().addItem(rodService.createGoldenFish(amount));
        sender.sendMessage("§aВыдано " + amount + " золотых рыбок игроку " + target.getName());
    }

    private void handleReload(CommandSender sender) {
        if (!admin(sender)) return;
        plugin.reloadConfig();
        sender.sendMessage("§aКонфиг RaskolFishing перезагружен.");
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("license", "rod", "fragment", "golden", "reload"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "license" -> filter(Arrays.asList("buy", "status", "setprice"), args[1]);
                case "rod" -> filter(List.of("give"), args[1]);
                case "fragment" -> filter(List.of("give"), args[1]);
                case "golden" -> filter(List.of("give"), args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rod") && args[1].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("VANILLA", "AMATEUR", "FISHERMAN", "SEA_LUCK"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("fragment") && args[1].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("AMATEUR", "FISHERMAN", "SEA_LUCK"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) out.add(s);
        }
        return out;
    }
}
