package always.io.kr.hide_and_seek;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    private final Hide_and_seek plugin;

    private Location pos1; // 산타 대기소
    private Location pos2; // 요정 시작점
    private final Set<UUID> exceptions = new HashSet<>();

    private boolean isRunning = false;
    private Player tagger; // 산타
    private final Set<UUID> citizens = new HashSet<>(); // 요정들
    private final Set<UUID> caughtPlayers = new HashSet<>(); // 잡힌 요정
    private int timeLeft = 300; // 5분

    public GameManager(Hide_and_seek plugin) {
        this.plugin = plugin;
    }

    // --- 설정 ---
    public void setPos1(Location loc) { this.pos1 = loc; }
    public void setPos2(Location loc) { this.pos2 = loc; }

    public void toggleException(Player p) {
        if (exceptions.contains(p.getUniqueId())) {
            exceptions.remove(p.getUniqueId());
            p.sendMessage(Hide_and_seek.PREFIX.append(Component.text("다시 요정 명단에 추가되었어요! 📝", NamedTextColor.GREEN)));
        } else {
            exceptions.add(p.getUniqueId());
            p.sendMessage(Hide_and_seek.PREFIX.append(Component.text("이번 게임에서는 빠지게 되었어요. 푹 쉬세요! ☕", NamedTextColor.YELLOW)));
        }
    }

    // --- 게임 시작 ---
    public void startGame(Player starter) {
        if (pos1 == null || pos2 == null) {
            starter.sendMessage(Hide_and_seek.PREFIX.append(Component.text("위치 설정(Pos1, Pos2)이 필요해요!", NamedTextColor.RED)));
            return;
        }
        if (isRunning) {
            starter.sendMessage(Hide_and_seek.PREFIX.append(Component.text("이미 게임이 진행 중이에요!", NamedTextColor.RED)));
            return;
        }

        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !exceptions.contains(p.getUniqueId()))
                .collect(Collectors.toList());

        if (players.size() < 2) {
            starter.sendMessage(Hide_and_seek.PREFIX.append(Component.text("최소 2명의 친구가 필요해요.", NamedTextColor.RED)));
            return;
        }

        isRunning = true;
        timeLeft = 300;
        caughtPlayers.clear();
        citizens.clear();

        Collections.shuffle(players);
        tagger = players.getFirst();

        for (Player p : players) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.setHealth(20);
            p.setFoodLevel(20);
            p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));

            if (p.equals(tagger)) {
                p.teleport(pos1);
                p.sendMessage(Hide_and_seek.PREFIX.append(Component.text("당신은 [산타]입니다! 15초 뒤 출발해요!", NamedTextColor.RED)));
                giveTaggerItems(p);
            } else {
                citizens.add(p.getUniqueId());
                p.teleport(pos2);
                p.sendMessage(Hide_and_seek.PREFIX.append(Component.text("당신은 [요정]입니다! 꼭꼭 숨으세요!", NamedTextColor.GREEN)));
            }
        }

        runGameTimer();

        // 15초 카운트다운
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isRunning || tagger == null || !tagger.isOnline()) return;
            tagger.teleport(pos2);

            broadcast(Component.text("🎅 메리 크리스마스! 산타가 도망간 요정들을 잡으러 왔어요!", NamedTextColor.RED, TextDecoration.BOLD));
            playSoundAll(1f, 1f);
        }, 15 * 20L);
    }

    // --- 타이머 및 승리 판정 ---
    private void runGameTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    this.cancel();
                    return;
                }

                // 1. 산타 승리 (모든 요정 잡힘)
                if (!citizens.isEmpty() && citizens.size() == caughtPlayers.size()) {
                    finishGame(true);
                    this.cancel();
                    return;
                }

                // 2. 요정 승리 (시간 종료)
                if (timeLeft <= 0) {
                    finishGame(false);
                    this.cancel();
                    return;
                }

                if (timeLeft == 60) {
                    broadcast(Component.text("⏰ 게임 시간이 1분 남았어요!", NamedTextColor.YELLOW));
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // --- 게임 종료 및 결과 처리 ---
    private void finishGame(boolean santaWon) {
        isRunning = false;
        Component titleMain;
        Component titleSub;
        Sound sound;

        if (santaWon) {
            // 산타 승리
            titleMain = Component.text("🎅 산타 승리!", NamedTextColor.RED, TextDecoration.BOLD);
            titleSub = Component.text("모든 요정이 선물 자루에 들어갔어요 🎁", NamedTextColor.YELLOW);
            sound = Sound.UI_TOAST_CHALLENGE_COMPLETE;
            broadcast(Component.text("🎅 산타가 모든 요정을 잡아서 승리했어요!", NamedTextColor.RED));
        } else {
            // 요정 승리
            titleMain = Component.text("🧝 요정 승리!", NamedTextColor.GREEN, TextDecoration.BOLD);
            titleSub = Component.text("산타가 지쳐서 선물 주기를 포기했어요 💨", NamedTextColor.AQUA);
            sound = Sound.ENTITY_FIREWORK_ROCKET_BLAST;
            broadcast(Component.text("🧝 요정들이 산타에게서 끝까지 도망쳐서 승리했어요!", NamedTextColor.GREEN));
        }

        // Title 띄우기
        Title title = Title.title(
                titleMain,
                titleSub,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(1000))
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
            p.playSound(p.getLocation(), sound, 1f, 1f);
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE); // 모드 복구
            p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
        }

        cleanup();
    }

    public void forceStopGame() {
        if (!isRunning) return;
        isRunning = false;
        broadcast(Component.text("관리자에 의해 게임이 종료되었습니다.", NamedTextColor.GRAY));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
        }
        cleanup();
    }

    private void cleanup() {
        tagger = null;
        citizens.clear();
        caughtPlayers.clear();
    }

    // --- 기능 로직 ---
    public void catchCitizen(Player p) {
        if (caughtPlayers.contains(p.getUniqueId())) return;

        caughtPlayers.add(p.getUniqueId());
        p.setGameMode(GameMode.SPECTATOR);

        broadcast(Component.text("🎁 ", NamedTextColor.RED)
                .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" 요정이 잡혔어요! (생존: " + getAliveCount() + "명)", NamedTextColor.RED)));

        p.sendTitlePart(net.kyori.adventure.title.TitlePart.TITLE, Component.text("잡혔다!", NamedTextColor.RED));
        p.sendTitlePart(net.kyori.adventure.title.TitlePart.SUBTITLE, Component.text("다른 요정들을 응원해주세요...", NamedTextColor.GRAY));
        p.playSound(p.getLocation(), Sound.ENTITY_SNOW_GOLEM_HURT, 1f, 1f);
    }

    public void reviveRandomCitizen(Player reviver) {
        if (caughtPlayers.isEmpty()) {
            reviver.sendMessage(Hide_and_seek.PREFIX.append(Component.text("아직 잡혀간 요정이 없어요! 😉", NamedTextColor.GREEN)));
            return;
        }

        List<UUID> list = new ArrayList<>(caughtPlayers);
        UUID luckyId = list.get(new Random().nextInt(list.size()));
        Player luckyPlayer = Bukkit.getPlayer(luckyId);

        if (luckyPlayer != null) {
            caughtPlayers.remove(luckyId);
            luckyPlayer.setGameMode(GameMode.ADVENTURE);
            luckyPlayer.teleport(pos2);

            broadcast(Component.text("✨ ", NamedTextColor.AQUA)
                    .append(Component.text(reviver.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" 요정이 ", NamedTextColor.AQUA))
                    .append(Component.text(luckyPlayer.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" 요정을 구했어요!", NamedTextColor.AQUA)));

            luckyPlayer.playSound(luckyPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
        }
    }

    // --- 아이템 ---
    private void giveTaggerItems(Player p) {
        ItemStack stick = new ItemStack(Material.BLAZE_ROD);
        ItemMeta stickMeta = stick.getItemMeta();
        stickMeta.displayName(Component.text("🍭 마법의 사탕 지팡이", NamedTextColor.RED));
        stickMeta.lore(List.of(Component.text("요정을 톡! 건드려 잡으세요.", NamedTextColor.GRAY)));
        stick.setItemMeta(stickMeta);

        ItemStack glow = new ItemStack(Material.GLOW_BERRIES, 3);
        ItemMeta glowMeta = glow.getItemMeta();
        glowMeta.displayName(Component.text("🔴 루돌프의 빨간 코 (우클릭)", NamedTextColor.GOLD));
        glowMeta.lore(List.of(Component.text("사용하면 숨은 요정들이 반짝거려요!", NamedTextColor.GRAY)));
        glow.setItemMeta(glowMeta);

        p.getInventory().addItem(stick, glow);
    }

    public ItemStack getKeyItem() {
        ItemStack key = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = key.getItemMeta();
        meta.displayName(Component.text("⭐ 크리스마스의 기적", NamedTextColor.YELLOW, TextDecoration.BOLD));
        meta.lore(List.of(Component.text("우클릭하여 잡힌요정을 구하세요!", NamedTextColor.WHITE)));
        key.setItemMeta(meta);
        return key;
    }

    // --- 유틸 ---
    public void broadcast(Component msg) {
        // 모든 메시지 앞에 깔끔한 Prefix 부착
        Bukkit.broadcast(Hide_and_seek.PREFIX.append(msg));
    }

    private void playSoundAll(float volume, float pitch) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, volume, pitch);
        }
    }

    // Getters
    public boolean isRunning() { return isRunning; }
    public Player getTagger() { return tagger; }
    public boolean isCitizen(Player p) { return citizens.contains(p.getUniqueId()); }
    public boolean isCaught(Player p) { return caughtPlayers.contains(p.getUniqueId()); }
    public int getTimeLeft() { return timeLeft; }
    public int getAliveCount() { return citizens.size() - caughtPlayers.size(); }
    public int getCaughtCount() { return caughtPlayers.size(); }
    public int getTotalCitizenCount() { return citizens.size(); }
}