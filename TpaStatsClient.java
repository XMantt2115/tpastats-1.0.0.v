package pl.tpastats;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TpaStatsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("tpastats");
    public static KeyBinding OPEN;

    @Override
    public void onInitializeClient() {
        TpaConfig.load();

        OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tpastats.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.tpastats"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.wasPressed()) {
                if (client.player != null && client.currentScreen == null && TpaConfig.get().onTargetServer()) {
                    client.setScreen(new TpaStatsScreen());
                }
            }
        });

        // Tylko wiadomości systemowe (nie czat graczy) - żeby nikt nie mógł sfałszować prośby wpisując tekst na czacie.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) TpaTracker.onMessage(message.getString());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TpaTracker.clear());
    }
}
