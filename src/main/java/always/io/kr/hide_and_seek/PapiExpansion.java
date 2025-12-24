package always.io.kr.hide_and_seek;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PapiExpansion extends PlaceholderExpansion {

    private final Hide_and_seek plugin;

    public PapiExpansion(Hide_and_seek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "has"; } // %has_...%

    @Override
    public @NotNull String getAuthor() { return "PluginAuthor"; }

    @Override
    public @NotNull String getVersion() { return "1.0"; }

    @Override // true를 리턴해야 PAPI에 등록됨
    public boolean canRegister() { return true; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        GameManager gm = plugin.getManager();

        if (!gm.isRunning()) {
            return "대기 중";
        }

        return switch (params.toLowerCase()) {
            case "time" -> {
                int sec = gm.getTimeLeft();
                yield String.format("%02d:%02d", sec / 60, sec % 60);
            }
            case "alive" -> String.valueOf(gm.getAliveCount());
            case "caught" -> String.valueOf(gm.getCaughtCount());
            case "total" -> String.valueOf(gm.getTotalCitizenCount());
            case "tagger" -> gm.getTagger() != null ? gm.getTagger().getName() : "없음";
            default -> null;
        };

    }
}