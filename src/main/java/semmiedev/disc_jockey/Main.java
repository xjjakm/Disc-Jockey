package semmiedev.disc_jockey;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import semmiedev.disc_jockey.gui.hud.PlaybackProgressOverlay;
import semmiedev.disc_jockey.gui.screen.DiscJockeyScreen;

import java.io.File;
import java.util.ArrayList;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "disc_jockey";
    public static final MutableComponent NAME = Component.literal("Disc Jockey");
    public static final Logger LOGGER = LogManager.getLogger("Disc Jockey");
    public static final ArrayList<ClientTickEvents.StartLevelTick> TICK_LISTENERS = new ArrayList<>();
    public static final Previewer PREVIEWER = new Previewer();
    public static final SongPlayer SONG_PLAYER = new SongPlayer();

    public static File songsFolder;
    public static Config config;
    public static ConfigHolder<Config> configHolder;

    @Override
    public void onInitializeClient() {
        configHolder = AutoConfig.register(Config.class, CustomConfigSerializer::new);
        config = configHolder.getConfig();

        ClientLifecycleEvents.CLIENT_STOPPING.register(_ -> {
            if (!config.rememberLastSelectedOnRestart && !config.lastSelectedSong.isEmpty()) {
                config.lastSelectedSong = "";
                configHolder.save();
            }
        });

        

        songsFolder = new File(FabricLoader.getInstance().getConfigDir()+File.separator+MOD_ID+File.separator+"songs");
        if (!songsFolder.isDirectory() && !songsFolder.mkdirs()) {
            LOGGER.warn("Failed to create songs folder: {}", songsFolder.getAbsolutePath());
        }

        SongLoader.loadSongs();

        //KeyBinding openScreenKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(MOD_ID+".key_bind.open_screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.category."+MOD_ID));
        // 修复按键绑定
        KeyMapping openScreenKeyBind = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                MOD_ID + ".key_bind.open_screen",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_J,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.START_CLIENT_TICK.register(new ClientTickEvents.StartTick() {
            private ClientLevel prevWorld;

            @Override
            public void onStartTick(@Nullable Minecraft client) {
                if (client == null) return;
                if (prevWorld != client.level) {
                    PREVIEWER.stop();
                    SONG_PLAYER.stop();
                }
                prevWorld = client.level;

                if (openScreenKeyBind.consumeClick()) {
                    if (SongLoader.loadingSongs) {
//                        client.gui.getChat().addMessage(Component.translatable(Main.MOD_ID+".still_loading").withStyle(ChatFormatting.RED));
                        client.gui.hud.getChat().addMessage(Component.translatable(Main.MOD_ID+".still_loading").withStyle(ChatFormatting.RED), null, GuiMessageSource.PLAYER, GuiMessageTag.chatError());
                        SongLoader.showToast = true;
                    } else {
                        client.gui.setScreen(new DiscJockeyScreen());
                    }
                }
            }
        });

        ClientTickEvents.START_LEVEL_TICK.register(world -> {
            for (ClientTickEvents.StartLevelTick listener : new java.util.ArrayList<>(TICK_LISTENERS)) listener.onStartTick(world);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> DiscjockeyCommand.register(dispatcher));

        ClientLoginConnectionEvents.DISCONNECT.register((_, _) -> {
            PREVIEWER.stop();
            SONG_PLAYER.stop();
        });

        HudElementRegistry.addLast(Identifier.withDefaultNamespace(Main.MOD_ID + "/" + "playback_progress"), new PlaybackProgressOverlay());
    }
}
