package pl.tpastats;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Przechowuje aktywne prośby TPA wykryte z czatu. */
public final class TpaTracker {
    public record Entry(String name, long time) {}

    private static final LinkedHashMap<String, Entry> REQUESTS = new LinkedHashMap<>();
    private static final Set<String> CHECKED = new HashSet<>();

    private TpaTracker() {}

    public static void onMessage(String text) {
        TpaConfig cfg = TpaConfig.get();
        if (!cfg.onTargetServer()) return;
        if (cfg.debugLogMessages) TpaStatsClient.LOGGER.info("[msg] {}", text);

        for (Pattern p : cfg.compiled()) {
            Matcher m = p.matcher(text);
            if (m.find() && m.groupCount() >= 1 && m.group(1) != null) {
                handle(m.group(1));
                return;
            }
        }
    }

    private static void handle(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (name.equalsIgnoreCase(mc.player.getName().getString())) return;

        ClientPlayNetworkHandler h = mc.getNetworkHandler();
        if (TpaConfig.get().requireOnline && (h == null || h.getPlayerListEntry(name) == null)) return;

        CHECKED.remove(name.toLowerCase());
        REQUESTS.remove(name.toLowerCase());
        REQUESTS.put(name.toLowerCase(), new Entry(name, System.currentTimeMillis()));

        mc.player.sendMessage(Text.literal("[TPA Stats] " + name + " chce się do Ciebie teleportować - naciśnij X")
                .formatted(Formatting.LIGHT_PURPLE), true);

        if (TpaConfig.get().autoOpen && mc.currentScreen == null) {
            mc.setScreen(new TpaStatsScreen());
        }
    }

    public static List<Entry> active() {
        long now = System.currentTimeMillis();
        long ttl = Math.max(5, TpaConfig.get().expireSeconds) * 1000L;
        REQUESTS.values().removeIf(e -> now - e.time() > ttl);
        List<Entry> list = new ArrayList<>(REQUESTS.values());
        list.sort((a, b) -> Long.compare(b.time(), a.time()));
        return list;
    }

    public static void remove(String name) {
        REQUESTS.remove(name.toLowerCase());
    }

    public static void markChecked(String name) {
        CHECKED.add(name.toLowerCase());
    }

    public static boolean isChecked(String name) {
        return CHECKED.contains(name.toLowerCase());
    }

    public static void clear() {
        REQUESTS.clear();
        CHECKED.clear();
    }
}
