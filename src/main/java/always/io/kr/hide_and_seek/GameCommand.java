package always.io.kr.hide_and_seek;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

public class GameCommand implements CommandExecutor, TabCompleter {

    private final Hide_and_seek plugin;

    public GameCommand(Hide_and_seek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (!(sender instanceof Player player)) return false;
        if (!player.isOp()) return false;

        if (args.length == 0) return false;

        GameManager gm = plugin.getManager();

        switch (args[0].toLowerCase()) {
            case "start":
                gm.startGame(player);
                break;
            case "end":
                if (gm.isRunning()) {
                    gm.forceStopGame();
                } else {
                    player.sendMessage(Hide_and_seek.PREFIX.append(Component.text("진행 중인 게임이 없어요.", NamedTextColor.YELLOW)));
                }
                break;
            case "position1":
                gm.setPos1(player.getLocation());
                player.sendMessage(Hide_and_seek.PREFIX.append(Component.text("🎅 산타 굴뚝(Pos1) 위치 설정 완료!", NamedTextColor.GREEN)));
                break;
            case "position2":
                gm.setPos2(player.getLocation());
                player.sendMessage(Hide_and_seek.PREFIX.append(Component.text("🧝 요정 마을(Pos2) 위치 설정 완료!", NamedTextColor.GREEN)));
                break;
            case "exception":
                if (args.length < 2) {
                    player.sendMessage(Hide_and_seek.PREFIX.append(Component.text("사용법: /has exception <닉네임>", NamedTextColor.RED)));
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target != null) {
                    gm.toggleException(target);
                } else {
                    player.sendMessage(Hide_and_seek.PREFIX.append(Component.text("그 친구는 지금 없어요.", NamedTextColor.RED)));
                }
                break;
            case "get":
                player.getInventory().addItem(gm.getKeyItem());
                player.sendMessage(Hide_and_seek.PREFIX.append(Component.text("⭐ 크리스마스의 기적(부활권)을 받았어요.", NamedTextColor.AQUA)));
                break;
            default:
                player.sendMessage(Hide_and_seek.PREFIX.append(Component.text("알 수 없는 명령어입니다.", NamedTextColor.RED)));
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