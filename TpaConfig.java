package pl.tpastats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Konfiguracja: .minecraft/config/tpastats.json */
public final class TpaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TpaConfig instance = new TpaConfig();

    /** Czy GUI ma się samo otworzyć po nowej prośbie TPA (domyślnie NIE - na anarchii to może przeszkadzać w walce). */
    public boolean autoOpen = false;
    /** Po ilu sekundach prośba znika z listy. */
    public int expireSeconds = 120;
    /** Akceptuj tylko nicki, które są aktualnie na liście graczy (chroni przed fałszywymi dopasowaniami). */
    public boolean requireOnline = true;
    /** Loguje każdą wiadomość systemową do latest.log - pomocne przy dopasowaniu wzorców. */
    public boolean debugLogMessages = false;
    /** Mod działa tylko na serwerach, których adres zawiera ten tekst. Puste = wszędzie. */
    public String serverFilter = "anarchia.gg";
    /** Komenda wysyłana po kliknięciu. {player} = nick. */
    public String statsCommand = "stats {player}";
    /** Wzorce regex wykrywające prośbę TPA. Grupa 1 = nick. */
    public List<String> patterns = new ArrayList<>(List.of(
            "(?i)\\b([A-Za-z0-9_]{3,16}) (?:has requested to teleport to you|wants to teleport to you|asks to teleport to you)",
            "(?i)\\b([A-Za-z0-9_]{3,16}) (?:wysłał|wysłała|wyslal|wyslala)[^.]*?(?:prośbę|prosbe|tpa)",
            "(?i)\\b([A-Za-z0-9_]{3,16}) (?:chce|prosi)[^.]*?teleport",
            "(?i)(?:prośba|prosba|tpa)[^.]*?\\b(?:od|from)\\s+([A-Za-z0-9_]{3,16})"
    ));

    private transient List<Pattern> compiled;

    public static TpaConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("tpastats.json");
    }

    public static void load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    TpaConfig c = GSON.fromJson(r, TpaConfig.class);
                    if (c != null) {
                        if (c.patterns == null) c.patterns = new ArrayList<>();
                        if (c.statsCommand == null || c.statsCommand.isBlank()) c.statsCommand = "stats {player}";
                        instance = c;
                    }
                }
            }
            save();
        } catch (Exception e) {
            TpaStatsClient.LOGGER.error("Nie udało się wczytać configu", e);
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            TpaStatsClient.LOGGER.error("Nie udało się zapisać configu", e);
        }
    }

    public List<Pattern> compiled() {
        if (compiled == null) {
            compiled = new ArrayList<>();
            for (String s : patterns) {
                try {
                    compiled.add(Pattern.compile(s));
                } catch (Exception e) {
                    TpaStatsClient.LOGGER.warn("Zły wzorzec regex: {}", s);
                }
            }
        }
        return compiled;
    }

    public boolean onTargetServer() {
        if (serverFilter == null || serverFilter.isBlank()) return true;
        ServerInfo s = MinecraftClient.getInstance().getCurrentServerEntry();
        return s != null && s.address != null && s.address.toLowerCase().contains(serverFilter.toLowerCase());
    }
}
