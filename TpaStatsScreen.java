package pl.tpastats;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ciemne GUI z fioletowym akcentem: lista graczy z TPA + przycisk /stats. */
public class TpaStatsScreen extends Screen {
    private static final int W = 470, H = 292;

    private static final int PANEL = 0xF2121118;
    private static final int SIDEBAR = 0xFF0C0B11;
    private static final int CARD = 0xFF1A1823;
    private static final int CARD_HOVER = 0xFF252236;
    private static final int ACCENT = 0xFF7C5CFF;
    private static final int ACCENT_H = 0xFF9A82FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8A869B;
    private static final int MUTED_L = 0xFFB9B5CC;
    private static final int GREEN = 0xFF4ADE80;
    private static final int DANGER = 0xFFFF5C7A;
    private static final int LINE = 0xFF221F30;

    private record Row(String name, long time, boolean checked) {}

    private record Hit(int x, int y, int w, int h, boolean inList, Runnable left, Runnable right) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final long openTime = System.currentTimeMillis();
    private final Map<String, Float> anim = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private int tab = 0;
    private float scroll = 0, scrollAnim = 0;
    private float fade = 0;
    private int listX, listY, listW, listH;

    public TpaStatsScreen() {
        super(Text.literal("TPA Stats"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        float t = MathHelper.clamp((System.currentTimeMillis() - openTime) / 240f, 0f, 1f);
        fade = 1f - (1f - t) * (1f - t) * (1f - t);

        ctx.fillGradient(0, 0, width, height, al(0xE0140C26, fade), al(0xF0050408, fade));

        int x = (width - W) / 2;
        int y = (height - H) / 2 + (int) ((1f - fade) * 14f);

        rounded(ctx, x - 1, y - 1, W + 2, H + 2, 9, al(0xFF2E2950, fade));
        rounded(ctx, x, y, W, H, 8, al(PANEL, fade));

        drawSidebar(ctx, x, y, mx, my);
        drawHeader(ctx, x, y);
        drawList(ctx, x, y, mx, my);
        drawFooter(ctx, x, y, mx, my);
    }

    private void drawSidebar(DrawContext ctx, int x, int y, int mx, int my) {
        int sx = x + 8, sy = y + 8, sw = 38, sh = H - 16;
        rounded(ctx, sx, sy, sw, sh, 8, al(SIDEBAR, fade));

        rounded(ctx, sx + 6, sy + 6, 26, 26, 7, al(ACCENT, fade));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("T").formatted(Formatting.BOLD),
                sx + 19, sy + 15, al(WHITE, fade));

        tabButton(ctx, sx + 6, sy + 48, "TPA", 0, mx, my);
        tabButton(ctx, sx + 6, sy + 80, "ALL", 1, mx, my);

        // przycisk zamknij
        int bx = sx + 6, by = sy + sh - 32;
        boolean hov = inside(mx, my, bx, by, 26, 26);
        float h = hv("close", hov);
        rounded(ctx, bx, by, 26, 26, 7, al(mix(0xFF16141F, 0xFF3A1F2B, h), fade));
        ctx.drawCenteredTextWithShadow(textRenderer, "×", bx + 13, by + 9, al(mix(MUTED_L, DANGER, h), fade));
        hits.add(new Hit(bx, by, 26, 26, false, this::close, null));
    }

    private void tabButton(DrawContext ctx, int bx, int by, String label, int idx, int mx, int my) {
        boolean hov = inside(mx, my, bx, by, 26, 26);
        float h = hv("tab" + idx, hov);
        boolean active = tab == idx;
        int bg = active ? ACCENT : mix(0xFF16141F, 0xFF262335, h);
        rounded(ctx, bx, by, 26, 26, 7, al(bg, fade));
        ctx.drawCenteredTextWithShadow(textRenderer, label, bx + 13, by + 9, al(active ? WHITE : MUTED_L, fade));
        if (idx == 0 && !TpaTracker.active().isEmpty()) {
            rounded(ctx, bx + 19, by - 3, 9, 9, 4, al(DANGER, fade));
        }
        hits.add(new Hit(bx, by, 26, 26, false, () -> {
            tab = idx;
            scroll = 0;
            scrollAnim = 0;
        }, null));
    }

    private void drawHeader(DrawContext ctx, int x, int y) {
        int cx = x + 60;

        var m = ctx.getMatrices();
        m.push();
        m.translate(cx, y + 14, 0);
        m.scale(1.7f, 1.7f, 1f);
        ctx.drawTextWithShadow(textRenderer, Text.literal("TPA ").formatted(Formatting.BOLD), 0, 0, al(WHITE, fade));
        ctx.drawTextWithShadow(textRenderer, Text.literal("STATS").formatted(Formatting.BOLD),
                textRenderer.getWidth("TPA ") + 2, 0, al(ACCENT_H, fade));
        m.pop();

        ctx.drawTextWithShadow(textRenderer, "anarchia.gg  ·  gracze, którzy teleportują się do Ciebie",
                cx, y + 36, al(MUTED, fade));

        // chip z liczbą próśb
        int n = TpaTracker.active().size();
        String s = "Prośby: " + n;
        int cw = textRenderer.getWidth(s) + 18;
        int chipX = x + W - 16 - cw;
        float pulse = n > 0 ? (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0)) : 0f;
        rounded(ctx, chipX, y + 16, cw, 18, 9, al(mix(0xFF1D1A2B, 0xFF2D2450, pulse), fade));
        ctx.drawTextWithShadow(textRenderer, s, chipX + 9, y + 21, al(n > 0 ? ACCENT_H : MUTED_L, fade));

        // etykieta sekcji
        ctx.drawTextWithShadow(textRenderer, tab == 0 ? "PRZYCHODZĄCE PROŚBY TPA" : "GRACZE ONLINE",
                cx, y + 56, al(MUTED, fade));
        ctx.fill(cx, y + 68, x + W - 16, y + 69, al(LINE, fade));
    }

    private void drawList(DrawContext ctx, int x, int y, int mx, int my) {
        listX = x + 60;
        listY = y + 74;
        listW = W - 76;
        listH = H - 74 - 48;

        List<Row> rows = rows();
        int pitch = 40;
        int total = rows.size() * pitch;
        float max = Math.max(0, total - listH);
        scroll = MathHelper.clamp(scroll, 0, max);
        scrollAnim += (scroll - scrollAnim) * 0.35f;
        int rw = max > 0 ? listW - 8 : listW;

        if (rows.isEmpty()) {
            String a = tab == 0 ? "Brak próśb o teleportację" : "Nikogo nie ma na liście";
            String b = tab == 0 ? "Gdy ktoś wyśle /tpa, pojawi się tutaj." : "Lista graczy jest pusta.";
            ctx.drawCenteredTextWithShadow(textRenderer, a, listX + listW / 2, listY + 56, al(MUTED_L, fade));
            ctx.drawCenteredTextWithShadow(textRenderer, b, listX + listW / 2, listY + 72, al(MUTED, fade));
            return;
        }

        ctx.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < rows.size(); i++) {
            int ry = listY + i * pitch - Math.round(scrollAnim);
            if (ry + 36 < listY || ry > listY + listH) continue;
            drawRow(ctx, rows.get(i), listX, ry, rw, mx, my);
        }
        ctx.disableScissor();

        if (max > 0) {
            int trackX = listX + listW - 4;
            ctx.fill(trackX, listY, trackX + 3, listY + listH, al(0xFF181622, fade));
            int thumbH = Math.max(18, (int) (listH * (listH / (float) total)));
            int thumbY = listY + (int) ((listH - thumbH) * (scrollAnim / max));
            rounded(ctx, trackX, thumbY, 3, thumbH, 1, al(ACCENT, fade));
        }
    }

    private void drawRow(DrawContext ctx, Row r, int rx, int ry, int rw, int mx, int my) {
        boolean mouseInList = my >= listY && my < listY + listH;
        boolean hov = mouseInList && inside(mx, my, rx, ry, rw, 36);
        float h = hv("row" + tab + r.name(), hov);

        rounded(ctx, rx, ry, rw, 36, 7, al(mix(CARD, CARD_HOVER, h), fade));
        if (h > 0.02f) ctx.fill(rx, ry + 9, rx + 2, ry + 27, al(ACCENT, fade * h));

        drawHead(ctx, r.name(), rx + 10, ry + 6, 24);

        ctx.drawTextWithShadow(textRenderer, r.name(), rx + 42, ry + 8, al(WHITE, fade));

        String sub = r.time() >= 0 ? "prośba TPA · " + ago(r.time()) : "online";
        ctx.drawTextWithShadow(textRenderer, sub, rx + 42, ry + 20, al(MUTED, fade));
        if (r.checked()) {
            ctx.drawTextWithShadow(textRenderer, "· sprawdzony",
                    rx + 42 + textRenderer.getWidth(sub) + 4, ry + 20, al(GREEN, fade));
        }

        // klik w cały wiersz = /stats
        String name = r.name();
        hits.add(new Hit(rx, ry, rw, 36, true, () -> runStats(name), () -> prefill(name)));

        // przycisk SPRAWDŹ
        int bw = 74, bh = 20, bx = rx + rw - bw - 8, by = ry + 8;
        float bhv = hv("btn" + tab + name, mouseInList && inside(mx, my, bx, by, bw, bh));
        rounded(ctx, bx, by, bw, bh, 6, al(mix(ACCENT, ACCENT_H, bhv), fade));
        ctx.drawCenteredTextWithShadow(textRenderer, "SPRAWDŹ", bx + bw / 2, by + 6, al(WHITE, fade));
        hits.add(new Hit(bx, by, bw, bh, true, () -> runStats(name), () -> prefill(name)));

        // przycisk usuń (tylko zakładka TPA)
        if (tab == 0) {
            int dx = bx - 26;
            float dh = hv("del" + name, mouseInList && inside(mx, my, dx, by, 20, bh));
            rounded(ctx, dx, by, 20, bh, 6, al(mix(0xFF262335, 0xFF4A2030, dh), fade));
            ctx.drawCenteredTextWithShadow(textRenderer, "×", dx + 10, by + 6, al(mix(MUTED_L, DANGER, dh), fade));
            hits.add(new Hit(dx, by, 20, bh, true, () -> TpaTracker.remove(name), null));
        }
    }

    private void drawHead(DrawContext ctx, String name, int x, int y, int size) {
        ClientPlayNetworkHandler h = client != null ? client.getNetworkHandler() : null;
        PlayerListEntry e = h != null ? h.getPlayerListEntry(name) : null;
        if (e != null) {
            PlayerSkinDrawer.draw(ctx, e.getSkinTextures(), x, y, size);
        } else {
            rounded(ctx, x, y, size, size, 5, al(0xFF2A2640, fade));
            ctx.drawCenteredTextWithShadow(textRenderer, name.substring(0, 1).toUpperCase(),
                    x + size / 2, y + 8, al(MUTED_L, fade));
        }
    }

    private void drawFooter(DrawContext ctx, int x, int y, int mx, int my) {
        int cx = x + 60;
        ctx.fill(cx, y + H - 40, x + W - 16, y + H - 39, al(LINE, fade));

        // przełącznik auto-otwierania
        boolean on = TpaConfig.get().autoOpen;
        int tx = cx, ty = y + H - 30;
        float v = hv("auto", on);
        rounded(ctx, tx, ty, 26, 14, 7, al(mix(0xFF2A2640, ACCENT, v), fade));
        rounded(ctx, tx + 2 + (int) (v * 12), ty + 2, 10, 10, 5, al(WHITE, fade));
        String label = "Auto-otwieranie przy TPA";
        ctx.drawTextWithShadow(textRenderer, label, tx + 34, ty + 3, al(on ? MUTED_L : MUTED, fade));
        hits.add(new Hit(tx, ty, 34 + textRenderer.getWidth(label), 14, false, () -> {
            TpaConfig.get().autoOpen = !TpaConfig.get().autoOpen;
            TpaConfig.save();
        }, null));

        String hint = "LPM – sprawdź  ·  PPM – wpisz w czacie";
        ctx.drawTextWithShadow(textRenderer, hint, x + W - 16 - textRenderer.getWidth(hint), ty + 3, al(MUTED, fade));
    }

    // ------------------------------------------------------------ logic

    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        if (tab == 0) {
            for (TpaTracker.Entry e : TpaTracker.active()) {
                out.add(new Row(e.name(), e.time(), TpaTracker.isChecked(e.name())));
            }
        } else if (client != null && client.getNetworkHandler() != null && client.player != null) {
            String self = client.player.getName().getString();
            List<String> names = new ArrayList<>();
            for (PlayerListEntry e : client.getNetworkHandler().getPlayerList()) {
                String n = e.getProfile().getName();
                if (n != null && !n.isEmpty() && !n.equalsIgnoreCase(self)) names.add(n);
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            for (String n : names) out.add(new Row(n, -1, TpaTracker.isChecked(n)));
        }
        return out;
    }

    private String commandFor(String name) {
        String cmd = TpaConfig.get().statsCommand.replace("{player}", name);
        return cmd.startsWith("/") ? cmd.substring(1) : cmd;
    }

    private void runStats(String name) {
        if (client == null || client.player == null) return;
        client.player.networkHandler.sendChatCommand(commandFor(name));
        TpaTracker.markChecked(name);
        close();
    }

    private void prefill(String name) {
        if (client == null) return;
        client.setScreen(new ChatScreen("/" + commandFor(name)));
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(mx, my)) continue;
            if (h.inList() && (my < listY || my >= listY + listH)) continue;
            if (button == 0 && h.left() != null) {
                h.left().run();
                return true;
            }
            if (button == 1 && h.right() != null) {
                h.right().run();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (float) (verticalAmount * 28);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (TpaStatsClient.OPEN != null && TpaStatsClient.OPEN.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------ helpers

    private float hv(String key, boolean on) {
        float v = anim.getOrDefault(key, 0f);
        v = MathHelper.clamp(v + (on ? 0.22f : -0.12f), 0f, 1f);
        anim.put(key, v);
        return v;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String ago(long time) {
        long s = Math.max(0, (System.currentTimeMillis() - time) / 1000);
        return s < 60 ? s + "s temu" : (s / 60) + "min temu";
    }

    private static int al(int color, float f) {
        int a = Math.max(5, (int) ((color >>> 24) * MathHelper.clamp(f, 0f, 1f)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, float t) {
        int a = (int) MathHelper.lerp(t, c1 >>> 24, c2 >>> 24);
        int r = (int) MathHelper.lerp(t, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(t, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(t, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Prostokąt z zaokrąglonymi rogami. */
    private static void rounded(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }
}
