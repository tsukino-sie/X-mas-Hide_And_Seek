package always.io.kr.hide_and_seek;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Hide_and_seek extends JavaPlugin {

    private static Hide_and_seek instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;

        // 매니저 초기화
        this.gameManager = new GameManager(this);

        // 명령어 & 이벤트 등록
        Objects.requireNonNull(getCommand("has")).setExecutor(new GameCommand(this));
        Objects.requireNonNull(getCommand("has")).setTabCompleter(new GameCommand(this));
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        // PlaceholderAPI 등록
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PapiExpansion(this).register();
        }

        getLogger().info("술래잡기 플러그인 활성화 완료!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isRunning()) {
            gameManager.stopGame("서버 종료로 인한 강제 중단");
        }
    }

    public static Hide_and_seek getInstance() { return instance; }
    public GameManager getManager() { return gameManager; }
}