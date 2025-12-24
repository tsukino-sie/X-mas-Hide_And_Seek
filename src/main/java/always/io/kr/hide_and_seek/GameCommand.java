package always.io.kr.hide_and_seek;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class GameCommand implements CommandExecutor, TabCompleter {

    private final Hide_and_seek plugin;

    public GameCommand(Hide_and_seek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (!(sender instanceof Player player)) return false;
        if (!player.isOp()) return false; // OP만 사용 가능

        if (args.length == 0) return false;

        GameManager gm = plugin.getManager();

        switch (args[0].toLowerCase()) {
            case "start":
                gm.startGame(player);
                break;
            case "end":
                gm.stopGame("관리자 강제 종료");
                break;
            case "position1":
                gm.setPos1(player.getLocation());
                player.sendMessage(Component.text("술래 대기소(Pos1) 설정 완료", NamedTextColor.GREEN));
                break;
            case "position2":
                gm.setPos2(player.getLocation());
                player.sendMessage(Component.text("시민 시작점(Pos2) 설정 완료", NamedTextColor.GREEN));
                break;
            case "exception":
                if (args.length < 2) {
                    player.sendMessage("사용법: /has exception <플레이어>");
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target != null) {
                    gm.toggleException(target);
                }
                break;
            case "get": // 열쇠 얻기
                player.getInventory().addItem(gm.getKeyItem());
                player.sendMessage(Component.text("부활의 열쇠를 지급받았습니다.", NamedTextColor.AQUA));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (args.length == 1) return Arrays.asList("start", "end", "position1", "position2", "exception", "get");
        return null;
    }
}