package always.io.kr.hide_and_seek;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Hide_and_seek extends JavaPlugin {

    private static Hide_and_seek instance;
    private GameManager gameManager;

    // ✨ Prefix: 이모지/볼드 제거, 색상만 적용 (빨강/초록 크리스마스 컬러)
    public static final Component PREFIX = Component.empty()
            .append(Component.text("[", NamedTextColor.RED))
            .append(Component.text("산타 술래잡기", NamedTextColor.GREEN))
            .append(Component.text("] ", NamedTextColor.RED));

    @Override
    public void onEnable() {
        instance = this;
        this.gameManager = new GameManager(this);

        // 명령어 및 이벤트 등록
        if (getCommand("has") != null) {
            GameCommand cmd = new GameCommand(this);
            Objects.requireNonNull(getCommand("has")).setExecutor(cmd);
            Objects.requireNonNull(getCommand("has")).setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        // PAPI 등록
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PapiExpansion(this).register();
        }

        getLogger().info("plugin Activate Successfully,");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isRunning()) {
            gameManager.forceStopGame();
        }
    }

    public static Hide_and_seek getInstance() { return instance; }
    public GameManager getManager() { return gameManager; }
}