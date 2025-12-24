package always.io.kr.hide_and_seek;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    private final Hide_and_seek plugin; // 메인 클래스 이름에 맞게 수정

    private Location pos1; // 산타 굴뚝 (대기소)
    private Location pos2; // 요정 마을 (게임장)
    private final Set<UUID> exceptions = new HashSet<>();

    private boolean isRunning = false;
    private Player tagger; // 산타
    private final Set<UUID> citizens = new HashSet<>(); // 요정들
    private final Set<UUID> caughtPlayers = new HashSet<>(); // 잡힌 요정
    private int timeLeft = 300;

    public GameManager(Hide_and_seek plugin) {
        this.plugin = plugin;
    }

    // --- 설정 ---
    public void setPos1(Location loc) { this.pos1 = loc; }
    public void setPos2(Location loc) { this.pos2 = loc; }

    public void toggleException(Player p) {
        if (exceptions.contains(p.getUniqueId())) {
            exceptions.remove(p.getUniqueId());
            p.sendMessage(Component.text("다시 요정 명단에 추가되었어요! 📝", NamedTextColor.GREEN));
        } else {
            exceptions.add(p.getUniqueId());
            p.sendMessage(Component.text("이번 놀이에서는 빠지게 되었어요. 푹 쉬세요! ☕", NamedTextColor.YELLOW));
        }
    }

    // --- 게임 시작 ---
    public void startGame(Player starter) {
        if (pos1 == null || pos2 == null) {
            starter.sendMessage(Component.text("산타 굴뚝(Pos1)과 요정 마을(Pos2) 위치를 먼저 정해주세요!", NamedTextColor.RED));
            return;
        }
        if (isRunning) {
            starter.sendMessage(Component.text("이미 크리스마스 놀이가 진행 중이에요!", NamedTextColor.RED));
            return;
        }

        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !exceptions.contains(p.getUniqueId()))
                .collect(Collectors.toList());

        if (players.size() < 2) {
            starter.sendMessage(Component.text("놀이를 하려면 최소 2명의 친구가 필요해요. 🥺", NamedTextColor.RED));
            return;
        }

        isRunning = true;
        timeLeft = 300;
        caughtPlayers.clear();
        citizens.clear();

        Collections.shuffle(players);
        tagger = players.get(0);

        for (Player p : players) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.setHealth(20);
            p.setFoodLevel(20);

            if (p.equals(tagger)) {
                // 산타 설정
                p.teleport(pos1);
                p.sendMessage(Component.text("당신은 [🎅 산타]입니다!", NamedTextColor.RED, TextDecoration.BOLD));
                p.sendMessage(Component.text("15초 뒤에 요정들에게 선물을 주러(잡으러) 갑니다! 준비하세요!", NamedTextColor.YELLOW));
                giveTaggerItems(p);
            } else {
                // 요정 설정
                citizens.add(p.getUniqueId());
                p.teleport(pos2);
                p.sendMessage(Component.text("당신은 [🧝 요정]입니다!", NamedTextColor.GREEN, TextDecoration.BOLD));
                p.sendMessage(Component.text("산타 할아버지에게 잡히지 않게 꼭꼭 숨으세요!", NamedTextColor.YELLOW));
            }
        }

        runGameTimer();

        // 15초 카운트다운 로직
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isRunning || tagger == null || !tagger.isOnline()) return;
            tagger.teleport(pos2);

            broadcast(Component.text("---------------------------------------", NamedTextColor.WHITE));
            broadcast(Component.text("🎅 메리 크리스마스! 산타가 마을에 도착했어요!", NamedTextColor.RED, TextDecoration.BOLD));
            broadcast(Component.text("요정들은 산타에게 잡히지 않게 도망치세요! 🎁", NamedTextColor.YELLOW));
            broadcast(Component.text("---------------------------------------", NamedTextColor.WHITE));

            tagger.playSound(tagger.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1f); // 종소리
        }, 15 * 20L);
    }

    // --- 게임 종료 ---
    public void stopGame(String reason) {
        isRunning = false;
        broadcast(Component.text("🎄 놀이 종료: " + reason, NamedTextColor.GOLD));

        if (tagger != null) tagger.getInventory().clear();
        for (UUID uuid : citizens) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.getInventory().clear();
        }

        tagger = null;
        citizens.clear();
        caughtPlayers.clear();
    }

    // --- 타이머 ---
    private void runGameTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    this.cancel();
                    return;
                }

                if (citizens.size() == caughtPlayers.size()) {
                    stopGame("🎅 산타의 승리! (모든 요정을 잡았어요)");
                    playSoundAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    this.cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    stopGame("🧝 요정들의 승리! (산타가 지쳐서 돌아갔어요)");
                    playSoundAll(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
                    this.cancel();
                    return;
                }

                // 1분 남았을 때 알림
                if (timeLeft == 60) {
                    broadcast(Component.text("⏰ 놀이 시간이 1분밖에 안 남았어요! 힘내세요!", NamedTextColor.AQUA));
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // --- 아이템 지급 (컨셉 변경) ---
    private void giveTaggerItems(Player p) {
        // 1. 잡는 도구 -> 사탕 지팡이
        ItemStack stick = new ItemStack(Material.BLAZE_ROD);
        ItemMeta stickMeta = stick.getItemMeta();
        stickMeta.displayName(Component.text("🍭 마법의 사탕 지팡이", NamedTextColor.RED));
        stickMeta.lore(List.of(Component.text("요정을 톡! 건드려서 선물 자루에 담으세요.", NamedTextColor.GRAY)));
        stick.setItemMeta(stickMeta);

        // 2. 발광 도구 -> 루돌프 코
        ItemStack glow = new ItemStack(Material.GLOW_BERRIES, 3);
        ItemMeta glowMeta = glow.getItemMeta();
        glowMeta.displayName(Component.text("🔴 루돌프의 빨간 코 (우클릭)", NamedTextColor.GOLD));
        glowMeta.lore(List.of(Component.text("사용하면 숨어있는 요정들이 반짝거려요!", NamedTextColor.GRAY)));
        glow.setItemMeta(glowMeta);

        p.getInventory().addItem(stick, glow);
    }

    // 열쇠 -> 별
    public ItemStack getKeyItem() {
        ItemStack key = new ItemStack(Material.NETHER_STAR); // 별 모양으로 변경
        ItemMeta meta = key.getItemMeta();
        meta.displayName(Component.text("⭐ 크리스마스의 기적", NamedTextColor.YELLOW, TextDecoration.BOLD));
        meta.lore(List.of(Component.text("우클릭하면 잡혀간 요정 친구 1명을", NamedTextColor.WHITE),
                Component.text("마법처럼 구해줄 수 있어요!", NamedTextColor.WHITE)));
        key.setItemMeta(meta);
        return key;
    }

    // --- 로직 ---
    public boolean isRunning() { return isRunning; }
    public Player getTagger() { return tagger; }
    public boolean isCitizen(Player p) { return citizens.contains(p.getUniqueId()); }
    public boolean isCaught(Player p) { return caughtPlayers.contains(p.getUniqueId()); }

    public void catchCitizen(Player p) {
        if (caughtPlayers.contains(p.getUniqueId())) return;

        caughtPlayers.add(p.getUniqueId());
        p.setGameMode(GameMode.SPECTATOR);

        // 귀여운 검거 메시지
        broadcast(Component.text("🎁 ", NamedTextColor.RED)
                .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" 요정이 산타에게 잡혀서 선물 자루에 들어갔어요!", NamedTextColor.RED)));

        p.sendTitlePart(net.kyori.adventure.title.TitlePart.TITLE, Component.text("잡혔다!", NamedTextColor.RED));
        p.sendTitlePart(net.kyori.adventure.title.TitlePart.SUBTITLE, Component.text("친구들이 구해주길 기다리세요...", NamedTextColor.GRAY));

        p.playSound(p.getLocation(), Sound.ENTITY_SNOW_GOLEM_HURT, 1f, 1f); // 눈사람 소리

        if (citizens.size() == caughtPlayers.size()) {
            stopGame("🎅 산타 승리!");
        }
    }

    public void reviveRandomCitizen(Player reviver) {
        if (caughtPlayers.isEmpty()) {
            reviver.sendMessage(Component.text("아직 잡혀간 친구가 없어요! 😉", NamedTextColor.GREEN));
            return;
        }

        List<UUID> list = new ArrayList<>(caughtPlayers);
        UUID luckyId = list.get(new Random().nextInt(list.size()));
        Player luckyPlayer = Bukkit.getPlayer(luckyId);

        if (luckyPlayer != null) {
            caughtPlayers.remove(luckyId);
            luckyPlayer.setGameMode(GameMode.ADVENTURE);
            luckyPlayer.teleport(pos2);

            // 부활 메시지
            broadcast(Component.text("✨ ", NamedTextColor.AQUA)
                    .append(Component.text(reviver.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" 요정이 기적을 일으켜 ", NamedTextColor.AQUA))
                    .append(Component.text(luckyPlayer.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" 요정을 구해줬어요!", NamedTextColor.AQUA)));

            luckyPlayer.playSound(luckyPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
        }
    }

    public void broadcast(Component msg) {
        Bukkit.broadcast(msg);
    }

    private void playSoundAll(Sound sound, float volume, float pitch) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), sound, volume, pitch);
        }
    }

    // PAPI 용 메소드들 유지...
    public int getTimeLeft() { return timeLeft; }
    public int getAliveCount() { return citizens.size() - caughtPlayers.size(); }
    public int getCaughtCount() { return caughtPlayers.size(); }
    public int getTotalCitizenCount() { return citizens.size(); }
}