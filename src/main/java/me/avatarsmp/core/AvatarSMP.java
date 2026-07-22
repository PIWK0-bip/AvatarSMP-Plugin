package me.avatarsmp.core;

import me.avatarsmp.core.commands.AvatarCommand;
import me.avatarsmp.core.gui.ElementSelectionGUI;
import me.avatarsmp.core.gui.GUIListener;
import me.avatarsmp.core.gui.GUIManager;
import me.avatarsmp.core.gui.SkillsGUI;
import me.avatarsmp.core.skill.EarthBoulderSkill;
import me.avatarsmp.core.update.UpdateManager;
import me.avatarsmp.core.data.PlayerData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AvatarSMP extends JavaPlugin implements Listener {

    public static final MiniMessage MM = MiniMessage.miniMessage();

    private DataManager dataManager;
    private CooldownManager cooldownManager;
    private ComboManager comboManager;
    private SkillManager skillManager;
    private BendingManager bendingManager;
    private InteractionListener interactionListener;
    private EnvironmentalListener environmentalListener;
    private EnergyManager energyManager;
    private ScoreboardManager scoreboardManager;
    private ExpBarManager expBarManager;
    private GUIManager guiManager;
    private UpdateManager updateManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Inicjalizacja menedżerów
        this.dataManager = new DataManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.comboManager = new ComboManager(this);
        this.skillManager = new SkillManager();
        this.bendingManager = new BendingManager(this, this.dataManager, this.cooldownManager, this.skillManager);
        this.interactionListener = new InteractionListener(this, this.bendingManager);
        this.environmentalListener = new EnvironmentalListener(this, this.dataManager);
        this.energyManager = new EnergyManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.expBarManager = new ExpBarManager(this);
        this.guiManager = new GUIManager(this, this.dataManager);
        this.updateManager = new UpdateManager(this);
        this.languageManager = new LanguageManager(this);

        this.bendingManager.setInteractionListener(this.interactionListener);
        this.bendingManager.setEnvironmentalListener(this.environmentalListener);
        this.bendingManager.setEnergyManager(this.energyManager);

        // Rejestracja listenerów eventów
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this, this.bendingManager, this.comboManager), this);
        Bukkit.getPluginManager().registerEvents(new ChiBlockListener(this, this.dataManager), this);
        Bukkit.getPluginManager().registerEvents(this.interactionListener, this);
        Bukkit.getPluginManager().registerEvents(this.environmentalListener, this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(this, this.dataManager, this.guiManager), this);
        Bukkit.getPluginManager().registerEvents(this.guiManager, this);
        Bukkit.getPluginManager().registerEvents(new XpListener(this, this.dataManager), this);

        // Rejestracja komendy /avatar oraz jej TabCompletera
            AvatarCommand avatarCommand = new AvatarCommand(this);
            if (getCommand("avatar") != null) {
                getCommand("avatar").setExecutor(avatarCommand);
                getCommand("avatar").setTabCompleter(avatarCommand);
            }

        // Ładowanie danych graczy online (np. w przypadku przeładowania pluginu)
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.dataManager.loadAsync(player.getUniqueId());
            }

            getLogger().info("AvatarSMP włączony!");
        
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this.dataManager), this);



    // 2. Rejestracja komendy /avatar oraz /a, /av wraz z TabCompleterem
            AvatarCommand avatarCmd = new AvatarCommand(this);
            var command = getCommand("avatar");
            if (command != null) {
                command.setExecutor(avatarCmd);
                command.setTabCompleter(avatarCmd);
            } else {
                getLogger().severe("Nie udalo sie zarejestrowac komendy /avatar! Sprawdz plugin.yml.");
            }

        }
        
        public void debug(String message) {
            if (getConfig().getBoolean("debug", false)) {
                getLogger().info("[DEBUG] " + message);
            }
        }
        
    // --- GETTERY ---

        public DataManager getDataManager() {
            return this.dataManager;
        }

        public CooldownManager getCooldownManager() {
            return this.cooldownManager;
        }

        public ComboManager getComboManager() {
            return this.comboManager;
        }

        public BendingManager getBendingManager() {
            return this.bendingManager;
        }

        public EnergyManager getEnergyManager() {
            return this.energyManager;
        }

        public ScoreboardManager getScoreboardManager() {
            return this.scoreboardManager;
        }

        public ExpBarManager getExpBarManager() {
            return this.expBarManager;
        }

        public GUIManager getGuiManager() {
            return this.guiManager;
        }

        public UpdateManager getUpdateManager() {
            return this.updateManager;
        }

        public LanguageManager getLanguageManager() {
            return this.languageManager;
        }

    @Override
    public void onDisable() {
        if (this.dataManager != null) {
            this.dataManager.saveAllSync();
        }
        if (this.energyManager != null) {
            this.energyManager.shutdown();
        }
        if (this.scoreboardManager != null) {
            this.scoreboardManager.shutdown();
        }
        if (this.expBarManager != null) {
            this.expBarManager.shutdown();
        }
        EarthBoulderSkill.restoreAll();
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("AvatarSMP wyłączony!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.dataManager.loadAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        PlayerData data = this.dataManager.getData(uuid);
        if (data != null) {
            this.dataManager.saveSync(data);
        }
        this.dataManager.remove(uuid);
        this.cooldownManager.clearPlayer(uuid);
        this.comboManager.clearPlayer(uuid);
        this.bendingManager.setAvatarState(uuid, false);
        this.energyManager.removeBossBar(uuid);
        this.scoreboardManager.remove(uuid);
        this.expBarManager.restore(event.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("start")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Ta komenda jest dostępna tylko dla graczy.");
                return true;
            }
            PlayerData data = this.dataManager.getData(player.getUniqueId());
            if (data == null) {
                player.sendMessage(MM.deserialize("<red>Twoje dane wciąż się wczytują, spróbuj ponownie za chwilę."));
                return true;
            }
            if (data.getElement() != Element.NONE) {
                player.sendMessage(MM.deserialize("<red>Twoja ścieżka jest już wybrana i jest permanentna."));
                return true;
            }
            ElementSelectionGUI.open(player);
            return true;
        }
        if (command.getName().equalsIgnoreCase("avatar")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "help" -> {
                    if (!sender.hasPermission("avatarsmp.player")) {
                        sender.sendMessage(MM.deserialize("<red>Nie masz uprawnień."));
                        return true;
                    }
                    sendHelp(sender);
                }
                case "menu", "choose" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("Ta komenda jest dostępna tylko dla graczy.");
                        return true;
                    }
                    PlayerData data = this.dataManager.getData(player.getUniqueId());
                    if (data == null) {
                        player.sendMessage(MM.deserialize("<red>Twoje dane wciąż się wczytują."));
                        return true;
                    }
                    if (data.getElement() != Element.NONE) {
                        player.sendMessage(MM.deserialize("<red>Twoja ścieżka jest już wybrana i jest permanentna."));
                        return true;
                    }
                    ElementSelectionGUI.open(player);
                }
                case "skills" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("Ta komenda jest dostępna tylko dla graczy.");
                        return true;
                    }
                    if (!player.hasPermission("avatarsmp.player")) {
                        player.sendMessage(MM.deserialize("<red>Nie masz uprawnień."));
                        return true;
                    }
                    PlayerData data = this.dataManager.getData(player.getUniqueId());
                    if (data == null || data.getElement() == Element.NONE) {
                        player.sendMessage(MM.deserialize("<red>Musisz najpierw wybrać swoją ścieżkę: /start"));
                        return true;
                    }
                    SkillsGUI.open(player, this.dataManager);
                }
                case "bind" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("Ta komenda jest dostepna tylko dla graczy.");
                        return true;
                    }
                    if (!player.hasPermission("avatarsmp.player")) {
                        player.sendMessage(MM.deserialize("<red>Nie masz uprawnien."));
                        return true;
                    }
                    PlayerData data = this.dataManager.getData(player.getUniqueId());
                    if (data == null || data.getElement() == Element.NONE || data.getElement() == Element.WARRIOR) {
                        player.sendMessage(MM.deserialize("<red>Ta funkcja jest dostepna tylko dla benderow zywiolow."));
                        return true;
                    }
                    this.guiManager.openBindGUI(player, data);
                }
                case "info" -> {
                    Player target;
                    if (args.length >= 2) {
                        target = Bukkit.getPlayerExact(args[1]);
                        if (target == null) {
                            sender.sendMessage(MM.deserialize("<red>Gracz nie jest online."));
                            return true;
                        }
                    } else if (sender instanceof Player player) {
                        target = player;
                    } else {
                        sender.sendMessage(MM.deserialize("<red>Podaj nick gracza: /avatar info <gracz>"));
                        return true;
                    }
                    PlayerData data = this.dataManager.getData(target.getUniqueId());
                    if (data == null) {
                        sender.sendMessage(MM.deserialize("<red>Brak danych dla tego gracza."));
                        return true;
                    }
                    sender.sendMessage(MM.deserialize(
                            "<gold><bold>─── Statystyki: " + target.getName() + " ───\n" +
                                    "<gray>Żywioł: <white>" + data.getElement().name() + "\n" +
                                    "<gray>Poziom: <white>" + data.getLevel() + " <gray>(XP: " + data.getXp() + "/" + this.bendingManager.xpRequiredForNextLevel(data.getLevel()) + ")\n" +
                                    "<gray>Specjalizacja: <white>" + data.getSpecialization().name() + "\n" +
                                    "<gray>Chi zablokowane: <white>" + (data.isChiBlocked() ? "Tak" : "Nie")));
                }
                case "reload" -> {
                    if (!sender.hasPermission("avatarsmp.admin")) {
                        sender.sendMessage(MM.deserialize("<red>Nie masz uprawnień."));
                        return true;
                    }
                    reloadConfig();
                    this.energyManager.reload();
                    sender.sendMessage(MM.deserialize("<green>Konfiguracja AvatarSMP przeładowana."));
                }
                case "admin" -> {
                    if (!sender.hasPermission("avatarsmp.admin")) {
                        sender.sendMessage(MM.deserialize("<red>Nie masz uprawnień."));
                        return true;
                    }
                    if (args.length < 2) {
                        sendAdminHelp(sender);
                        return true;
                    }
                    handleAdmin(sender, args);
                }
                default -> sender.sendMessage(MM.deserialize("<red>Nieznana podkomenda. Użyj /avatar help"));
            }
            return true;
        }
        return false;
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        String adminSub = args[1].toLowerCase();
        switch (adminSub) {
            case "help" -> sendAdminHelp(sender);
            case "state" -> {
                if (args.length < 3) {
                    sender.sendMessage(MM.deserialize("<red>Użycie: /avatar admin state <nick>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(MM.deserialize("<red>Gracz nie jest online."));
                    return;
                }
                new AvatarStateTask(this, target).start();
                target.sendMessage(MM.deserialize("<gold><bold>Wchodzisz w Stan Avatara!"));
                sender.sendMessage(MM.deserialize("<gold>Aktywowano Stan Avatara dla " + target.getName() + "."));
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(MM.deserialize("<red>Użycie: /avatar admin set <nick> <zywiol>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(MM.deserialize("<red>Gracz nie jest online."));
                    return;
                }
                Element element;
                try {
                    element = Element.valueOf(args[3].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(MM.deserialize("<red>Nieznany żywioł. Dostępne: WATER, FIRE, EARTH, AIR, WARRIOR"));
                    return;
                }
                PlayerData targetData = this.dataManager.getData(target.getUniqueId());
                if (targetData == null) {
                    sender.sendMessage(MM.deserialize("<red>Dane gracza wciąż się wczytują."));
                    return;
                }
                targetData.setElement(element);
                this.dataManager.saveAsync(targetData);
                Sound grantSound = switch (element) {
                    case FIRE -> Sound.ENTITY_ENDER_DRAGON_GROWL;
                    case WATER -> Sound.ITEM_BUCKET_FILL;
                    case EARTH -> Sound.BLOCK_STONE_BREAK;
                    case AIR -> Sound.ENTITY_PHANTOM_FLAP;
                    default -> Sound.ENTITY_PLAYER_LEVELUP;
                };
                target.playSound(target.getLocation(), grantSound, 1.0f, 1.0f);
                if (element == Element.EARTH) {
                    target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 80, 1, 1, 1, 0.2,
                            Material.STONE.createBlockData());
                } else {
                    Particle particle = switch (element) {
                        case FIRE -> Particle.FLAME;
                        case WATER -> Particle.SPLASH;
                        case AIR -> Particle.CLOUD;
                        default -> Particle.EXPLOSION;
                    };
                    target.getWorld().spawnParticle(particle, target.getLocation().add(0, 1, 0), 80, 1, 1, 1, 0.2);
                }
                target.showTitle(Title.title(
                        MM.deserialize("<gold><bold>NOWA MOC!"),
                        MM.deserialize("<white>Otrzymałeś moc: " + element.name())));
                target.sendMessage(MM.deserialize("<gold><bold>Administrator nadał Ci moc: <white>" + element.name()));
                sender.sendMessage(MM.deserialize("<green>Nadano " + target.getName() + " moc " + element.name() + "."));
            }
            case "reset" -> {
                if (args.length < 3) {
                    sender.sendMessage(MM.deserialize("<red>Użycie: /avatar admin reset <nick>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(MM.deserialize("<red>Gracz nie jest online."));
                    return;
                }
                PlayerData targetData = this.dataManager.getData(target.getUniqueId());
                if (targetData == null) {
                    sender.sendMessage(MM.deserialize("<red>Dane gracza wciąż się wczytują."));
                    return;
                }
                targetData.setElement(Element.NONE);
                targetData.setLevel(1);
                targetData.setXp(0);
                targetData.setSpecialization(Specialization.NONE);
                this.dataManager.saveAsync(targetData);
                target.playSound(target.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.7f);
                target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation().add(0, 1, 0), 100, 1, 1, 1, 0.15);
                target.showTitle(Title.title(
                        MM.deserialize("<red><bold>UTRACIŁEŚ MOC"),
                        MM.deserialize("<gray>Twoja ścieżka bendingu została usunięta.")));
                target.sendMessage(MM.deserialize("<red><bold>Twoja moc została usunięta przez administratora."));
                sender.sendMessage(MM.deserialize("<green>Usunięto moc gracza " + target.getName() + "."));
            }
            case "xp" -> {
                if (args.length < 5) {
                    sender.sendMessage(MM.deserialize("<red>Użycie: /avatar admin xp <add|set> <nick> <ilosc>"));
                    return;
                }
                String mode = args[2].toLowerCase();
                Player target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    sender.sendMessage(MM.deserialize("<red>Gracz nie jest online."));
                    return;
                }
                PlayerData targetData = this.dataManager.getData(target.getUniqueId());
                if (targetData == null) {
                    sender.sendMessage(MM.deserialize("<red>Dane gracza wciąż się wczytują."));
                    return;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[4]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(MM.deserialize("<red>Ilość musi być liczbą."));
                    return;
                }
                if (mode.equals("set")) {
                    targetData.setXp(0);
                    targetData.setLevel(1);
                }
                if (mode.equals("add") || mode.equals("set")) {
                    this.bendingManager.grantXp(target, amount);
                } else {
                    sender.sendMessage(MM.deserialize("<red>Nieznany tryb. Użyj add lub set."));
                    return;
                }
                sender.sendMessage(MM.deserialize("<green>Zmodyfikowano XP gracza " + target.getName() + "."));
            }
            default -> sendAdminHelp(sender);
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(MM.deserialize("""
                <dark_red><bold>▬▬▬▬▬▬▬ AVATAR ADMIN ▬▬▬▬▬▬▬
                <yellow>/avatar admin help <gray>- ta lista
                <yellow>/avatar admin set <nick> <zywiol> <gray>- nadaje moc
                <yellow>/avatar admin reset <nick> <gray>- usuwa moc
                <yellow>/avatar admin xp add <nick> <ilosc> <gray>- dodaje XP
                <yellow>/avatar admin xp set <nick> <ilosc> <gray>- ustawia XP
                <yellow>/avatar admin state <nick> <gray>- aktywuje Stan Avatara
                <dark_red>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("avatar")) {
            return null;
        }
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("help", "menu", "choose", "skills", "bind", "info", "reload", "admin")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    completions.add(option);
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(online.getName());
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            for (String option : List.of("help", "set", "reset", "xp", "state")) {
                if (option.startsWith(args[1].toLowerCase())) {
                    completions.add(option);
                }
            }
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")
                && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset") || args[1].equalsIgnoreCase("state"))) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(online.getName());
                }
            }
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("xp")) {
            for (String option : List.of("add", "set")) {
                if (option.startsWith(args[2].toLowerCase())) {
                    completions.add(option);
                }
            }
            return completions;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("xp")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
                    completions.add(online.getName());
                }
            }
            return completions;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("set")) {
            for (Element element : Element.values()) {
                if (element.name().toLowerCase().startsWith(args[3].toLowerCase())) {
                    completions.add(element.name().toLowerCase());
                }
            }
            return completions;
        }
        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MM.deserialize("""
                         <gradient:#8ec5fc:#e0f7ff><bold>✦  AVATAR SMP  ✦</bold></gradient>
                <dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                   <aqua>▪ <click:suggest_command:/start><white>/start</white></click> <gray>» wybór ścieżki bendingu
                   <aqua>▪ <click:suggest_command:/avatar skills><white>/avatar skills</white></click> <gray>» kompendium umiejętności
                   <aqua>▪ <click:suggest_command:/avatar bind><white>/avatar bind</white></click> <gray>» przypisywanie mocy do hotbara
                   <aqua>▪ <click:suggest_command:/avatar menu><white>/avatar menu</white></click> <gray>» menu wyboru ścieżki
                   <aqua>▪ <click:suggest_command:/avatar info><white>/avatar info</white></click> <aqua>[gracz] <gray>» statystyki żywiołu

                    <aqua>• <white>LPM/PPM <gray>» użycie wybranej mocy
                    <aqua>• <white>1-9 <gray>» wybór przypisanego slotu

                <dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                   <aqua>Woda <white>• Ogień • Ziemia • Powietrze
                <dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"""));
    }
}