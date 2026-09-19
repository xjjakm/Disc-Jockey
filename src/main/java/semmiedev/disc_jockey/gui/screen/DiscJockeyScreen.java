package semmiedev.disc_jockey.gui.screen;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NonNull;
import semmiedev.disc_jockey.*;
import semmiedev.disc_jockey.gui.PreviewTimeSliderWidget;
import semmiedev.disc_jockey.gui.SongListWidget;
import semmiedev.disc_jockey.gui.SongTimeSliderWidget;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class DiscJockeyScreen extends Screen {
    private static final int MAX_CONTENT_WIDTH = 480;
    private int contentX;
    private int contentWidth;
    private static final MutableComponent
            SELECT_SONG = Component.translatable(Main.MOD_ID+".screen.select_song"),
            PLAY = Component.translatable(Main.MOD_ID+".screen.play"),
            PLAY_STOP = Component.translatable(Main.MOD_ID+".screen.play.stop"),
            PREVIEW = Component.translatable(Main.MOD_ID+".screen.preview"),
            PREVIEW_STOP = Component.translatable(Main.MOD_ID+".screen.preview.stop"),
            DROP_HINT = Component.translatable(Main.MOD_ID+".screen.drop_hint").withStyle(ChatFormatting.GRAY),
            SONGSTATE_PLAYING = Component.translatable(Main.MOD_ID+".screen.songstate.playing").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_PAUSED = Component.translatable(Main.MOD_ID+".screen.songstate.paused").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_FINISHED = Component.translatable(Main.MOD_ID+".screen.songstate.finished").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_STOPPED = Component.translatable(Main.MOD_ID+".screen.songstate.stopped").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_TUNING = Component.translatable(Main.MOD_ID+".screen.songstate.tuning").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            PLEASE_SELECT_SONG = Component.translatable(Main.MOD_ID+".screen.please_select_song").withStyle((style) -> style.withItalic(true)),
            CONFIG = Component.translatable(Main.MOD_ID+".screen.config"),
            MODE_SINGLE = Component.translatable(Main.MOD_ID+".screen.mode_single"),
            MODE_LIST = Component.translatable(Main.MOD_ID+".screen.mode_list"),
            MODE_RANDOM = Component.translatable(Main.MOD_ID+".screen.mode_random"),
            MODE_STOP = Component.translatable(Main.MOD_ID+".screen.mode_stop"),
            PLAYBACK_TITLE = Component.translatable(Main.MOD_ID+".screen.playback_title"),
            PREVIEW_TITLE = Component.translatable(Main.MOD_ID+".screen.preview_title")
    ;

    private StringWidget songTitle;
    private StringWidget songState;
    private CycleButton<Boolean> playPauseButton;
    private Button playModeButton;
    private SongTimeSliderWidget timeBar;
    private StringWidget previewTitle;
    private StringWidget previewState;
    private PreviewTimeSliderWidget previewTimeBar;
    private CycleButton<Boolean> previewPlayButton;
    private Button previewPlayModeButton;

    private SongListWidget songListWidget;
    private Button playButton, previewButton;
    public boolean shouldFilter;
    private String query = "";
    public SongLoader.SongFolder currentFolder = null;
    private boolean hasAutoScrolled = false;

    public DiscJockeyScreen() {
        super(Main.NAME);
    }

    @Override
    protected void init() {
        contentWidth = Math.min(MAX_CONTENT_WIDTH, width);
        contentX = (width - contentWidth) / 2;

        shouldFilter = true;
        currentFolder = null;
        hasAutoScrolled = false;

        if (!Main.config.autoScrollToLastSelected && !Main.config.lastSelectedSong.isEmpty()) {
            Main.config.lastSelectedSong = "";
            Main.configHolder.save();
        }

        if (Main.config.autoScrollToLastSelected && !Main.config.lastSelectedSong.isEmpty()) {
            for (Song song : SongLoader.SONGS) {
                if (song.filePath.equals(Main.config.lastSelectedSong)) {
                    if (song.folder != null) {
                        SongLoader.SongFolder parent = findParentFolder(song.folder);
                        if (parent != null || SongLoader.FOLDERS.contains(song.folder)) {
                            currentFolder = song.folder;
                        }
                    }
                    break;
                }
            }
        }

        songListWidget = new SongListWidget(minecraft, contentX + contentWidth / 2 - 10, height - 64 - 32, 12, 20, this);
        songListWidget.setX(contentX + contentWidth / 2);
        addRenderableWidget(songListWidget);

        int rowY1 = height - 61;
        int rowY2 = height - 31;
        int btnW1 = 68;
        int btnGap = 8;
        int btnTotal = btnW1 * 3 + btnGap * 2;
        int btnStartX = contentX + (contentWidth - btnTotal) / 2;

        playButton = Button.builder(PLAY, _button -> {
            if (Main.SONG_PLAYER.running) Main.SONG_PLAYER.stop();
            else {
                SongListWidget.SongEntry entry = songListWidget.getSelectedSongOrNull();
                if (entry != null) {
                    Main.SONG_PLAYER.start(entry.song);
                }
            }
        }).bounds(btnStartX, rowY1, btnW1, 20).build();
        addRenderableWidget(playButton);

        previewButton = Button.builder(PREVIEW, _button -> {
            if (Main.PREVIEWER.running) {
                Main.PREVIEWER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelectedSongOrNull();
                if (entry != null) Main.PREVIEWER.start(entry.song);
            }
        }).bounds(btnStartX + btnW1 + btnGap, rowY1, btnW1, 20).build();
        addRenderableWidget(previewButton);

        addRenderableWidget(Button.builder(Component.translatable(Main.MOD_ID+".screen.blocks"), _button -> {
            SongListWidget.SongEntry entry = songListWidget.getSelectedSongOrNull();
            if (entry != null) {
                try {
                    SongLoader.ensureSongLoaded(entry.song);
                } catch (IOException e) {
                    Main.LOGGER.error("Failed to load song for blocks overlay: {}", entry.song.fileName, e);
                    return;
                }

                minecraft.gui.setScreen(null);

                java.util.HashMap<net.minecraft.world.level.block.Block, Integer> blockCounts = new java.util.HashMap<>();
                for (Note note : entry.song.uniqueNotes) {
                    net.minecraft.world.level.block.Block block = Note.INSTRUMENT_BLOCKS.get(note.instrument());
                    blockCounts.put(block, blockCounts.getOrDefault(block, 0) + 1);
                }

                minecraft.gui.hud.getChat().addMessage(Component.translatable(Main.MOD_ID + ".screen.blocks.header").withStyle(ChatFormatting.YELLOW), null, net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER, net.minecraft.client.multiplayer.chat.GuiMessageTag.system());
                minecraft.gui.hud.getChat().addMessage(Component.translatable(Main.MOD_ID + ".screen.blocks.song", entry.song.fileName), null, net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER, net.minecraft.client.multiplayer.chat.GuiMessageTag.system());
                minecraft.gui.hud.getChat().addMessage(Component.translatable(Main.MOD_ID + ".screen.blocks.total", entry.song.uniqueNotes.size()), null, net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER, net.minecraft.client.multiplayer.chat.GuiMessageTag.system());
                
                for (java.util.Map.Entry<net.minecraft.world.level.block.Block, Integer> entry2 : blockCounts.entrySet()) {
                    minecraft.gui.hud.getChat().addMessage(Component.literal("  " + entry2.getKey().getName().getString() + " × " + entry2.getValue()).withStyle(ChatFormatting.GRAY), null, net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER, net.minecraft.client.multiplayer.chat.GuiMessageTag.system());
                }
            }
        }).bounds(btnStartX + (btnW1 + btnGap) * 2, rowY1, btnW1, 20).build());

        EditBox searchBar = new EditBox(font, contentX + 78, rowY2, contentWidth - 88, 20, Component.empty());
        searchBar.setHint(Component.translatable(Main.MOD_ID+".screen.search").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)));
        searchBar.setResponder(query -> {
            query = query.toLowerCase().replaceAll("\\s", "");
            if (this.query.equals(query)) return;
            this.query = query;
            shouldFilter = true;
        });
        addRenderableWidget(searchBar);

        int playbackY = 14;
        int lineH = 12;
        int titleH = 14;
        int sliderH = 14;
        int btnH = 16;
        int panelPad = 4;

        addRenderableWidget(new StringWidget(contentX + 10, playbackY + panelPad, contentWidth / 2 - 20, titleH, PLAYBACK_TITLE, getFont()));

        songState = new StringWidget(contentX + 10, playbackY + panelPad + titleH, contentWidth / 2 - 20, lineH, Component.empty(), getFont());
        addRenderableWidget(songState);
        songTitle = new StringWidget(contentX + 10, playbackY + panelPad + titleH + lineH, contentWidth / 2 - 20, lineH, Component.empty(), getFont());
        addRenderableWidget(songTitle);
        timeBar = new SongTimeSliderWidget(contentX + 10, playbackY + panelPad + titleH + lineH + lineH, contentWidth / 2 - 20, sliderH);
        addRenderableWidget(timeBar);
        int buttonY = playbackY + panelPad + titleH + lineH + lineH + sliderH + 2;

        Button prevSongButton = Button.builder(Component.literal("⏮"), _button -> Main.SONG_PLAYER.playPrevSong())
                .pos(contentX + contentWidth / 4 - 45, buttonY)
                .size(btnH, btnH)
                .build();
        addRenderableWidget(prevSongButton);

        playPauseButton = CycleButton.builder((value) -> Component.literal(value ? "⏸" : "▶"), Main.SONG_PLAYER.running)
                .displayOnlyValue()
                .withValues(true, false)
                .create(contentX + contentWidth / 4 - 25, buttonY, btnH, btnH, Component.empty(), (_button, value) -> {
            if (value) {
                if (Main.SONG_PLAYER.song != null && Main.SONG_PLAYER.didSongReachEnd) {
                    Main.SONG_PLAYER.start(Main.SONG_PLAYER.song);
                } else if (Main.SONG_PLAYER.song != null) {
                    Main.SONG_PLAYER.running = true;
                }
            } else {
                Main.SONG_PLAYER.running = false;
            }
        });
        addRenderableWidget(playPauseButton);

        Button nextSongButton = Button.builder(Component.literal("⏭"), _button -> Main.SONG_PLAYER.playNextSong())
                .pos(contentX + contentWidth / 4 + 5, buttonY)
                .size(btnH, btnH)
                .build();
        addRenderableWidget(nextSongButton);

        Button stopButton = Button.builder(Component.literal("⏹"), _button -> Main.SONG_PLAYER.stop())
                .pos(contentX + contentWidth / 4 + 25, buttonY)
                .size(btnH, btnH)
                .build();
        addRenderableWidget(stopButton);

        playModeButton = Button.builder(getPlayModeText(), _button -> {
            SongPlayer.PlayMode currentMode = Main.SONG_PLAYER.getPlayMode();
            SongPlayer.PlayMode nextMode;
            switch (currentMode) {
                case SINGLE_LOOP -> nextMode = SongPlayer.PlayMode.LIST_LOOP;
                case LIST_LOOP -> nextMode = SongPlayer.PlayMode.RANDOM;
                case RANDOM -> nextMode = SongPlayer.PlayMode.STOP_AFTER;
                case STOP_AFTER -> nextMode = SongPlayer.PlayMode.SINGLE_LOOP;
                default -> nextMode = SongPlayer.PlayMode.STOP_AFTER;
            }
            Main.SONG_PLAYER.setPlayMode(nextMode);
            playModeButton.setMessage(getPlayModeText());
        }).pos(contentX + contentWidth / 4 + 50, buttonY)
                .size(60, btnH)
                .build();
        addRenderableWidget(playModeButton);

        int playbackHeight = titleH + lineH + lineH + sliderH + 2 + btnH + panelPad;
        int previewY = playbackY + playbackHeight + 6;
        addRenderableWidget(new StringWidget(contentX + 10, previewY + panelPad, contentWidth / 2 - 20, titleH, PREVIEW_TITLE, getFont()));

        previewState = new StringWidget(contentX + 10, previewY + panelPad + titleH, contentWidth / 2 - 20, lineH, Component.empty(), getFont());
        addRenderableWidget(previewState);
        previewTitle = new StringWidget(contentX + 10, previewY + panelPad + titleH + lineH, contentWidth / 2 - 20, lineH, Component.empty(), getFont());
        addRenderableWidget(previewTitle);
        previewTimeBar = new PreviewTimeSliderWidget(contentX + 10, previewY + panelPad + titleH + lineH + lineH, contentWidth / 2 - 20, sliderH);
        addRenderableWidget(previewTimeBar);
        int previewButtonY = previewY + panelPad + titleH + lineH + lineH + sliderH + 2;

        Button previewPrevButton = Button.builder(Component.literal("⏮"), _button -> Main.PREVIEWER.playPrevSong())
                .pos(contentX + contentWidth / 4 - 45, previewButtonY)
                .size(btnH, btnH)
                .build();
        addRenderableWidget(previewPrevButton);

        previewPlayButton = CycleButton.builder((value) -> Component.literal(value ? "⏸" : "▶"), Main.PREVIEWER.running)
                .displayOnlyValue()
                .withValues(true, false)
                .create(contentX + contentWidth / 4 - 25, previewButtonY, btnH, btnH, Component.empty(), (_button, value) -> Main.PREVIEWER.running = value && Main.PREVIEWER.getSong() != null);
        addRenderableWidget(previewPlayButton);

        Button previewNextButton = Button.builder(Component.literal("⏭"), _button -> Main.PREVIEWER.playNextSong())
                .pos(contentX + contentWidth / 4 + 5, previewButtonY)
                .size(btnH, btnH)
                .build();
        addRenderableWidget(previewNextButton);

        Button previewStopButton = Button.builder(Component.literal("⏹"), _button -> Main.PREVIEWER.stop())
                .pos(contentX + contentWidth / 4 + 25, previewButtonY)
                .size(btnH, btnH)
                .build();
        addRenderableWidget(previewStopButton);

        previewPlayModeButton = Button.builder(getPreviewPlayModeText(), _button -> {
            Previewer.PlayMode currentMode = Main.PREVIEWER.getPlayMode();
            Previewer.PlayMode nextMode;
            switch (currentMode) {
                case SINGLE_LOOP -> nextMode = Previewer.PlayMode.LIST_LOOP;
                case LIST_LOOP -> nextMode = Previewer.PlayMode.RANDOM;
                case RANDOM -> nextMode = Previewer.PlayMode.STOP_AFTER;
                case STOP_AFTER -> nextMode = Previewer.PlayMode.SINGLE_LOOP;
                default -> nextMode = Previewer.PlayMode.STOP_AFTER;
            }
            Main.PREVIEWER.setPlayMode(nextMode);
            previewPlayModeButton.setMessage(getPreviewPlayModeText());
        }).pos(contentX + contentWidth / 4 + 50, previewButtonY)
                .size(60, btnH)
                .build();
        addRenderableWidget(previewPlayModeButton);

        Button configButton = Button.builder(CONFIG, (_button) -> minecraft.gui.setScreen(AutoConfigClient.getConfigScreen(Config.class, this).get()))
                .pos(contentX + 10, rowY2)
                .size(60, 20)
                .build();
        addRenderableWidget(configButton);
    }

    private static Component getPlaybackStateText() {
        boolean running = Main.SONG_PLAYER.running;
        boolean tuned = Main.SONG_PLAYER.tuner.isTuned();
        boolean didSongReachEnd = Main.SONG_PLAYER.didSongReachEnd;

        if(!running) {
            if (didSongReachEnd) {
                return SONGSTATE_FINISHED;
            } else if (Main.SONG_PLAYER.getSongElapsedSeconds() == 0.0) {
                return SONGSTATE_STOPPED;
            } else {
                return SONGSTATE_PAUSED;
            }
        } else {
            if (!tuned) {
                return SONGSTATE_TUNING;
            } else {
                return SONGSTATE_PLAYING;
            }
        }
    }

    private static Component getPreviewStateText() {
        boolean running = Main.PREVIEWER.running;
        if (!running) {
            if (Main.PREVIEWER.getSongElapsedSeconds() == 0.0) {
                return SONGSTATE_STOPPED;
            } else {
                return SONGSTATE_PAUSED;
            }
        } else {
            return SONGSTATE_PLAYING;
        }
    }

    private Component getPreviewPlayModeText() {
        return switch (Main.PREVIEWER.getPlayMode()) {
            case SINGLE_LOOP -> MODE_SINGLE;
            case LIST_LOOP -> MODE_LIST;
            case RANDOM -> MODE_RANDOM;
            case STOP_AFTER -> MODE_STOP;
        };
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int playbackY = 14;
        int playbackHeight = 76; // 14+12+12+14+2+16+4(pad)
        int halfWidth = contentWidth / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, AbstractSelectionList.INWORLD_MENU_LIST_BACKGROUND, contentX + 5, playbackY, halfWidth, playbackY + playbackHeight, halfWidth - 10, playbackHeight, 32, 32);

        int previewY = playbackY + playbackHeight + 6;
        int previewHeight = 76;
        context.blit(RenderPipelines.GUI_TEXTURED, AbstractSelectionList.INWORLD_MENU_LIST_BACKGROUND, contentX + 5, previewY, halfWidth, previewY + previewHeight, halfWidth - 10, previewHeight, 32, 32);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        context.text(font, DROP_HINT, contentX + contentWidth / 2, 5, 0xFFFFFF);
        context.text(font, SELECT_SONG, contentX + (contentWidth / 4 * 3), 20, 0xFFFFFF);
    }

    @Override
    public void tick() {
        songState.setMessage(getPlaybackStateText());
        timeBar.update();
        playPauseButton.setValue(Main.SONG_PLAYER.song != null && Main.SONG_PLAYER.running);
        songTitle.setMessage(Main.SONG_PLAYER.song != null ? Component.literal(Main.SONG_PLAYER.song.displayName) : PLEASE_SELECT_SONG);

        previewState.setMessage(getPreviewStateText());
        previewTimeBar.update();
        previewPlayButton.setValue(Main.PREVIEWER.getSong() != null && Main.PREVIEWER.running);
        previewTitle.setMessage(Main.PREVIEWER.getSong() != null ? Component.literal(Main.PREVIEWER.getSong().displayName) : PLEASE_SELECT_SONG);

        previewButton.setMessage(Main.PREVIEWER.running ? PREVIEW_STOP : PREVIEW);
        playButton.setMessage(Main.SONG_PLAYER.running ? PLAY_STOP : PLAY);

        if (shouldFilter) {
            shouldFilter = false;
            songListWidget.setScrollAmount(0);
            java.util.List<SongListWidget.Entry> newEntries = new java.util.ArrayList<>();
            boolean empty = query.isEmpty();

            if (currentFolder == null) {
                for (SongLoader.SongFolder folder : SongLoader.FOLDERS) {
                    if (empty || folder.name.toLowerCase().contains(query)) {
                        if (folder.entry == null) {
                            folder.entry = new SongListWidget.FolderEntry(folder, songListWidget);
                        } else {
                            folder.entry.songListWidget = songListWidget;
                        }
                        newEntries.add(folder.entry);
                    }
                }
            } else {
                SongListWidget.FolderEntry parentEntry = new SongListWidget.FolderEntry(null, songListWidget);
                parentEntry.displayName = "..";
                newEntries.add(parentEntry);

                for (SongLoader.SongFolder subFolder : currentFolder.subFolders) {
                    if (empty || subFolder.name.toLowerCase().contains(query)) {
                        if (subFolder.entry == null) {
                            subFolder.entry = new SongListWidget.FolderEntry(subFolder, songListWidget);
                        } else {
                            subFolder.entry.songListWidget = songListWidget;
                        }
                        newEntries.add(subFolder.entry);
                    }
                }
            }

            java.util.List<Song> songsToShow = currentFolder == null ?
                    SongLoader.SONGS.stream()
                            .filter(song -> song.folder == null)
                            .toList() :
                    currentFolder.songs.stream()
                            .filter(song -> song.folder == currentFolder)
                            .toList();

            for (Song song : songsToShow) {
                if (song.entry.favorite && (empty || song.searchableFileName.contains(query) || song.searchableName.contains(query))) {
                    song.entry.songListWidget = songListWidget;
                    newEntries.add(song.entry);
                }
            }

            for (Song song : songsToShow) {
                if (!song.entry.favorite && (empty || song.searchableFileName.contains(query) || song.searchableName.contains(query))) {
                    song.entry.songListWidget = songListWidget;
                    newEntries.add(song.entry);
                }
            }

            SongListWidget.SongEntry previouslySelectedEntry = songListWidget.getSelectedSongOrNull();

            Song selectedSongFromState = null;
            for (Song song : songsToShow) {
                if (song.entry.selected) {
                    selectedSongFromState = song;
                    break;
                }
            }

            songListWidget.safeReplaceEntries(newEntries);

            for (SongListWidget.Entry entry : newEntries) {
                entry.setSelected(false);
            }

            Song selectedSong = null;
            if (previouslySelectedEntry != null) {
                for (Song song : songsToShow) {
                    if (song == previouslySelectedEntry.song) {
                        selectedSong = song;
                        break;
                    }
                }
            }

            if (selectedSong == null && selectedSongFromState != null) {
                selectedSong = selectedSongFromState;
            }

            if (selectedSong == null && Main.config.autoScrollToLastSelected && !Main.config.lastSelectedSong.isEmpty()) {
                for (Song song : songsToShow) {
                    if (song.filePath.equals(Main.config.lastSelectedSong)) {
                        selectedSong = song;
                        break;
                    }
                }
            }

            if (selectedSong != null) {
                if (Main.config.autoScrollToLastSelected && !hasAutoScrolled) {
                    hasAutoScrolled = true;
                    songListWidget.setSelected(selectedSong.entry);
                    int entryIndex = newEntries.indexOf(selectedSong.entry);
                    if (entryIndex >= 0) {
                        double itemHeight = songListWidget.getItemHeight();
                        double scrollAmount = entryIndex * itemHeight - (songListWidget.getHeight() - itemHeight) / 2.0;
                        if (scrollAmount < 0) scrollAmount = 0;
                        songListWidget.setScrollAmount(scrollAmount);
                    }
                } else {
                    double currentScroll = songListWidget.scrollAmount();
                    songListWidget.setSelected(selectedSong.entry);
                    songListWidget.setScrollAmount(currentScroll);
                }
            }
        }
    }

    public SongLoader.SongFolder findParentFolder(SongLoader.SongFolder folder) {
        if (folder == null) return null;

        if (SongLoader.FOLDERS.contains(folder)) {
            return null;
        }

        for (SongLoader.SongFolder rootFolder : SongLoader.FOLDERS) {
            if (rootFolder.subFolders.contains(folder)) {
                return rootFolder;
            }
            SongLoader.SongFolder found = findParentInSubfolders(rootFolder, folder);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private SongLoader.SongFolder findParentInSubfolders(SongLoader.SongFolder parent, SongLoader.SongFolder target) {
        for (SongLoader.SongFolder subFolder : parent.subFolders) {
            if (subFolder == target) {
                return parent;
            }
            SongLoader.SongFolder found = findParentInSubfolders(subFolder, target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        String string = paths.stream().map(Path::getFileName).map(Path::toString).collect(joining(", "));
        if (string.length() > 300) string = string.substring(0, 300)+"...";

        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                paths.forEach(path -> {
                    try {
                        File file = path.toFile();

                        if (SongLoader.SONGS.stream().anyMatch(input -> input.fileName.equalsIgnoreCase(file.getName()))) return;

                        Song song = SongLoader.loadSong(file);
                        if (song != null) {
                            File destFile = Main.songsFolder.toPath().resolve(file.getName()).toFile();
                            Files.copy(path, destFile.toPath());
                            song.filePath = destFile.getPath();
                            song.folder = null;
                            SongLoader.SONGS.add(song);
                        }
                    } catch (IOException exception) {
                        Main.LOGGER.warn("Failed to copy song file from {} to {}", path, Main.songsFolder.toPath(), exception);
                    }
                });

                SongLoader.sort();
            }
            minecraft.gui.setScreen(this);
        }, Component.translatable(Main.MOD_ID+".screen.drop_confirm"), Component.literal(string)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        new Thread(() -> Main.configHolder.save()).start();
    }

    private Component getPlayModeText() {
        return switch (Main.SONG_PLAYER.getPlayMode()) {
            case SINGLE_LOOP -> MODE_SINGLE;
            case LIST_LOOP -> MODE_LIST;
            case RANDOM -> MODE_RANDOM;
            case STOP_AFTER -> MODE_STOP;
        };
    }
}
