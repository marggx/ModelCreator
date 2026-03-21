package dev.marggx.mcreator.ui;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.enums.PrefabRootDirectory;
import com.hypixel.hytale.builtin.buildertools.prefablist.AssetPrefabFileProvider;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerModule;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.browser.FileListProvider;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import dev.marggx.mcreator.data.extras.Model;
import dev.marggx.mcreator.services.HytaleService;
import dev.marggx.mcreator.services.MapperService;
import dev.marggx.mcreator.utils.Logger;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SavePage extends InteractiveCustomUIPage<SavePage.PageData> {
    private static final Logger LOGGER = Logger.get();
    private static final Value<String> BUTTON_HIGHLIGHTED = Value.ref("Pages/BasicTextButton.ui", "SelectedLabelStyle");

    private BlockSelection selection;
    private List<Model> models = new ObjectArrayList<>();
    private List<Model> unselectedModels = new ObjectArrayList<>();
    private Path browserRoot;
    private Path browserCurrent;
    private String selectedPath;
    @Nonnull
    private String browserSearchQuery = "";
    @Nonnull
    private final AssetPrefabFileProvider assetProvider = new AssetPrefabFileProvider();
    private boolean inAssetsRoot = false;
    @Nonnull
    private Path assetsCurrentDir = Paths.get("");

    public SavePage(@Nonnull PlayerRef playerRef, BlockSelection selection) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SavePage.PageData.CODEC);
        this.selection = selection;
        this.models = MapperService.get().getBlockymodelsAndEntitiesFromBlockSelection(selection);
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Save.ui");
        uiCommandBuilder.set("#Main #SelectedEntities #Label.Text", Message.translation("mcreator.ui.save.selectedEntitiesLabel").param("count", selection.getEntityCount()));
        int counter = 0;

        if (models == null) {
            buildError(uiCommandBuilder, Message.translation("mcreator.ui.error.noEntities").getAnsiMessage());
            uiCommandBuilder.set("#Settings.Visible", false);
            return;
        }
        for (Model model : models) {
            uiCommandBuilder.append("#Main #SelectedEntities #Entities", "Pages/EntityEntry.ui");
            uiCommandBuilder.set("#Main #SelectedEntities #Entities[" + counter + "] #Label.Text", model.id());
            Holder<EntityStore> holder = model.holder();
            NetworkId netId = holder.getComponent(NetworkId.getComponentType());
            if (netId == null) {
                continue;
            }
            uiCommandBuilder.set("#Main #SelectedEntities #Entities[" + counter + "] #CheckBox.TooltipText", String.valueOf(netId.getId()));
            uiEventBuilder.addEventBinding(
                    CustomUIEventBindingType.ValueChanged, "#Main #SelectedEntities #Entities[" + counter + "] #CheckBox",
                    new EventData()
                            .append(PageData.ACTION, PageData.Action.EntitySelection.name())
                            .append(PageData.NETWORK_ID, String.valueOf(netId.getId()))
                            .append(PageData.CHECKED, "#Main #SelectedEntities #Entities[" + counter + "] #CheckBox.Value")
            );
            counter++;
        }
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.SelectedTabChanged, "#Tabs", new EventData().append(PageData.ACTION, PageData.Action.TabSelection.name()).append(PageData.TAB, "#Tabs.SelectedTab"));

        if (AssetModule.get().getAssetPacks().size() == 1) {
            buildError(uiCommandBuilder, Message.translation("mcreator.ui.error.createPack").getAnsiMessage());
        }
        boolean firstPack = AssetModule.get().getAssetPacks().size() != 1;
        String firstEntry = "";
        List<DropdownEntryInfo> entries = new ArrayList<>();
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (firstPack) {
                firstPack = false;
                continue;
            }
            if (firstEntry.isEmpty()) {
                firstEntry = pack.getName();
            }
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(pack.getName()), pack.getName()));
        }
        uiCommandBuilder.set("#PackDropdown.Entries", entries);
        uiCommandBuilder.set("#PackDropdown.Value", firstEntry);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", new EventData().append(PageData.ACTION, PageData.Action.Cancel.name()));
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#GenerateButton",
                new EventData()
                        .append(PageData.ACTION, PageData.Action.Generate.name())
                        .append(PageData.PACK, "#Pack #PackDropdown.Value")
                        .append(PageData.NAME, "#Name #NameInput.Value")
                        .append(PageData.CREATE_ITEM, "#CreateItem #CheckBox.Value")
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BrowseButton",
                new EventData().append(PageData.ACTION, PageData.Action.OpenBrowser.name())
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BrowserPage #BrowserContent #RootSelector",
                new EventData()
                        .append("Action", PageData.Action.BrowserRootChanged.name())
                        .append("@BrowserRoot", "#BrowserPage #BrowserContent #RootSelector.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#BrowserPage #BrowserContent #SearchInput",
                new EventData()
                        .append("Action", PageData.Action.BrowserSearch.name())
                        .append("@BrowserSearch", "#BrowserPage #BrowserContent #SearchInput.Value"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BrowserPage #ConfirmButton",
                new EventData().append("Action", PageData.Action.ConfirmBrowser.name())
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BrowserPage #CancelButton",
                new EventData().append("Action", PageData.Action.CancelBrowser.name())
        );
        uiCommandBuilder.set("#BrowserPage.Visible", false);
    }

    private void buildError(UICommandBuilder uiCommandBuilder, String message) {
        buildError(uiCommandBuilder, message, true);
    }

    private void buildError(UICommandBuilder uiCommandBuilder, String message, boolean disableGenerateButton) {
        uiCommandBuilder.set("#SelectionNotification.Text", message);
        uiCommandBuilder.set("#SelectionNotification.Visible", true);
        uiCommandBuilder.set("#GenerateButton.Disabled", disableGenerateButton);
    }

    private void hideError(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#SelectionNotification.Text", "");
        uiCommandBuilder.set("#SelectionNotification.Visible", false);
        uiCommandBuilder.set("#GenerateButton.Disabled", false);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        UICommandBuilder uiBuilder = new UICommandBuilder();
        switch (data.action) {
            case TabSelection: {
                if (data.tab.equals("Selection")) {
                    uiBuilder.set("#Main #Selection.Visible", true);
                    uiBuilder.set("#Main #Prefab.Visible", false);
                } else {
                    uiBuilder.set("#Main #Selection.Visible", false);
                    uiBuilder.set("#Main #Prefab.Visible", true);
                }
                hideError(uiBuilder);
                this.sendUpdate(uiBuilder);
                break;
            }
            case Cancel: {
                playerComponent.getPageManager().setPage(ref, store, Page.None);
                break;
            }

            case EntitySelection: {
                if (data.checked) {
                    for (Model model : unselectedModels) {
                        NetworkId netId = model.holder().getComponent(NetworkId.getComponentType());
                        if (netId != null && String.valueOf(netId.getId()).equals(data.networkId)) {
                            hideError(uiBuilder);
                            models.add(model);
                            unselectedModels.remove(model);
                            break;
                        }
                    }
                } else {
                    for (Model model : models) {
                        NetworkId netId = model.holder().getComponent(NetworkId.getComponentType());
                        if (netId != null && String.valueOf(netId.getId()).equals(data.networkId)) {
                            models.remove(model);
                            unselectedModels.add(model);

                            if (models.isEmpty()) {
                                buildError(uiBuilder, Message.translation("mcreator.ui.error.noEntities").getAnsiMessage());
                            }
                            break;
                        }
                    }
                }
                uiBuilder.set("#Main #SelectedEntities #Label.Text", Message.translation("mcreator.ui.save.selectedEntitiesLabel").param("count", models.size()));
                this.sendUpdate(uiBuilder);
                break;
            }
            case Generate: {
                if (models.isEmpty()) {
                    buildError(uiBuilder, Message.translation("mcreator.ui.error.noEntities").getAnsiMessage());
                    this.sendUpdate(uiBuilder);
                    break;
                }

                if (data.name == null || data.name.isEmpty()) {
                    buildError(uiBuilder, Message.translation("mcreator.ui.error.noName").getAnsiMessage(), false);
                    this.sendUpdate(uiBuilder);
                    break;
                }


                boolean created = MapperService.get().createBlockymodelFromBlockSelection(models, selection, data.pack, data.name, data.createItem);
                if (created) {
                    playerComponent.getPageManager().setPage(ref, store, Page.None);
                    NotificationUtil.sendNotificationToUniverse(Message.translation("mcreator.ui.save.success").param("pack", data.pack), NotificationStyle.Success);
                    playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(new ItemStack(HytaleService.get().createValidItemName(data.name)));
                } else {
                    playerComponent.getPageManager().setPage(ref, store, Page.None);
                    NotificationUtil.sendNotificationToUniverse(Message.translation("mcreator.ui.save.error").param("pack", data.pack), NotificationStyle.Danger);
                }
                break;
            }

            case OpenBrowser: {
                this.inAssetsRoot = true;
                this.assetsCurrentDir = Paths.get("");
                this.browserRoot = Paths.get("Assets");
                this.browserCurrent = Paths.get("");
                this.selectedPath = null;
                this.browserSearchQuery = "";
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                commandBuilder.set("#MainPage.Visible", false);
                commandBuilder.set("#BrowserPage.Visible", true);
                List<DropdownEntryInfo> roots = this.buildBrowserRootEntries();
                commandBuilder.set("#BrowserPage #BrowserContent #RootSelector.Entries", roots);
                commandBuilder.set("#BrowserPage #BrowserContent #RootSelector.Value", "Assets");
                commandBuilder.set("#BrowserPage #BrowserContent #SearchInput.Value", "");
                this.buildBrowserList(commandBuilder, eventBuilder);
                this.sendUpdate(commandBuilder, eventBuilder, false);
                break;
            }
            case BrowserNavigate: {
                if (data.browserFile == null) {
                    return;
                }

                String fileName = data.browserFile;
                if (this.inAssetsRoot) {
                    this.handleAssetsNavigation(fileName);
                } else {
                    this.handleRegularNavigation(fileName);
                }
                break;
            }
            case BrowserRootChanged: {
                if (data.browserRootStr == null) {
                    return;
                }

                if (!this.isAllowedBrowserRoot(data.browserRootStr)) {
                    return;
                }

                this.inAssetsRoot = "Assets".equals(data.browserRootStr);
                this.assetsCurrentDir = Paths.get("");
                if (this.inAssetsRoot) {
                    this.browserRoot = Paths.get("Assets");
                    this.browserCurrent = Paths.get("");
                } else {
                    this.browserRoot = this.findActualRootPath(data.browserRootStr);
                    if (this.browserRoot == null) {
                        this.browserRoot = Path.of(data.browserRootStr);
                    }

                    this.browserCurrent = this.browserRoot.getFileSystem().getPath("");
                }

                this.selectedPath = null;
                this.browserSearchQuery = "";
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                commandBuilder.set("#BrowserPage #BrowserContent #SearchInput.Value", "");
                this.buildBrowserList(commandBuilder, eventBuilder);
                this.sendUpdate(commandBuilder, eventBuilder, false);
                break;
            }
            case BrowserSearch: {
                this.browserSearchQuery = data.browserSearchStr != null ? data.browserSearchStr.trim().toLowerCase() : "";
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildBrowserList(commandBuilder, eventBuilder);
                this.sendUpdate(commandBuilder, eventBuilder, false);
                break;
            }
            case ConfirmBrowser: {
                String pathToSet;
                pathToSet = this.getCurrentBrowserPath();
                UICommandBuilder commandBuilder = new UICommandBuilder();
                commandBuilder.set("#MainPage #PrefabSelection #Input.Value", pathToSet);

                commandBuilder.set("#BrowserPage.Visible", false);
                commandBuilder.set("#MainPage.Visible", true);
                this.sendUpdate(commandBuilder);
                break;
            }
            case CancelBrowser: {
                UICommandBuilder commandBuilder = new UICommandBuilder();
                commandBuilder.set("#BrowserPage.Visible", false);
                commandBuilder.set("#MainPage.Visible", true);
                this.sendUpdate(commandBuilder);
            }
        }
    }


    private void handleAssetsNavigation(@Nonnull String fileName) {
        if ("..".equals(fileName)) {
            if (!this.assetsCurrentDir.toString().isEmpty()) {
                Path parent = this.assetsCurrentDir.getParent();
                this.assetsCurrentDir = parent != null ? parent : Paths.get("");
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildBrowserList(commandBuilder, eventBuilder);
                this.sendUpdate(commandBuilder, eventBuilder, false);
            }
        } else {
            String currentDirStr = this.assetsCurrentDir.toString().replace('\\', '/');
            String targetVirtualPath = currentDirStr.isEmpty() ? fileName : currentDirStr + "/" + fileName;
            Path resolvedPath = this.assetProvider.resolveVirtualPath(targetVirtualPath);
            if (resolvedPath == null) {
                this.sendUpdate();
            } else {
                if (Files.isDirectory(resolvedPath)) {
                    this.assetsCurrentDir = Paths.get(targetVirtualPath);
                    this.selectedPath = targetVirtualPath + "/";
                    UICommandBuilder commandBuilder = new UICommandBuilder();
                    UIEventBuilder eventBuilder = new UIEventBuilder();
                    this.buildBrowserList(commandBuilder, eventBuilder);
                    this.sendUpdate(commandBuilder, eventBuilder, false);
                } else {
                    this.selectedPath = targetVirtualPath;
                    UICommandBuilder commandBuilder = new UICommandBuilder();
                    commandBuilder.set("#BrowserPage #CurrentPath.Text", "Assets/" + targetVirtualPath);
                    this.sendUpdate(commandBuilder);
                }
            }
        }
    }

    private void handleRegularNavigation(@Nonnull String fileName) {
        Path file = this.browserRoot.resolve(this.browserCurrent).resolve(fileName);
        if (!PathUtil.isChildOf(this.browserRoot, file)) {
            this.sendUpdate();
        } else {
            if (Files.isDirectory(file)) {
                this.browserCurrent = PathUtil.relativize(this.browserRoot, file);
                String pathStr = this.browserCurrent.toString().replace('\\', '/');
                this.selectedPath = pathStr.isEmpty() ? "/" : (pathStr.endsWith("/") ? pathStr : pathStr + "/");
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                this.buildBrowserList(commandBuilder, eventBuilder);
                this.sendUpdate(commandBuilder, eventBuilder, false);
            } else {
                this.selectedPath = PathUtil.relativize(this.browserRoot, file).toString().replace('\\', '/');
                UICommandBuilder commandBuilder = new UICommandBuilder();
                commandBuilder.set("#BrowserPage #CurrentPath.Text", this.selectedPath);
                this.sendUpdate(commandBuilder);
            }
        }
    }

    @Nonnull
    private String getCurrentBrowserPath() {
        if (this.selectedPath != null) {
            return this.selectedPath;
        } else if (this.inAssetsRoot) {
            String currentDirStr = this.assetsCurrentDir.toString().replace('\\', '/');
            return currentDirStr.isEmpty() ? "/" : (currentDirStr.endsWith("/") ? currentDirStr : currentDirStr + "/");
        } else {
            String pathStr = this.browserCurrent.toString().replace('\\', '/');
            return pathStr.isEmpty() ? "/" : (pathStr.endsWith("/") ? pathStr : pathStr + "/");
        }
    }

    private void buildBrowserList(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear("#BrowserPage #BrowserContent #FileList");
        if (this.inAssetsRoot) {
            this.buildAssetsBrowserList(commandBuilder, eventBuilder);
        } else {
            this.buildRegularBrowserList(commandBuilder, eventBuilder);
        }
    }

    private void buildAssetsBrowserList(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        String currentDirStr = this.assetsCurrentDir.toString().replace('\\', '/');
        String displayPath;
        if (currentDirStr.isEmpty()) {
            displayPath = "Assets";
        } else {
            String[] parts = currentDirStr.split("/", 2);
            String packName = parts[0];
            String subPath = parts.length > 1 ? "/" + parts[1] : "";
            if ("HytaleAssets".equals(packName)) {
                displayPath = packName + subPath;
            } else {
                displayPath = "Mods/" + packName + subPath;
            }
        }

        commandBuilder.set("#BrowserPage #CurrentPath.Text", displayPath);
        List<FileListProvider.FileEntry> entries = this.assetProvider.getFiles(this.assetsCurrentDir, this.browserSearchQuery);
        int buttonIndex = 0;
        if (!currentDirStr.isEmpty() && this.browserSearchQuery.isEmpty()) {
            commandBuilder.append("#BrowserPage #BrowserContent #FileList", "Pages/BasicTextButton.ui");
            commandBuilder.set("#BrowserPage #BrowserContent #FileList[0].Text", "../");
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BrowserPage #BrowserContent #FileList[0]",
                    new EventData().append("Action", PageData.Action.BrowserNavigate.name()).append("File", "..")
            );
            buttonIndex++;
        }

        for (FileListProvider.FileEntry entry : entries) {
            String displayText = entry.isDirectory() ? entry.displayName() + "/" : entry.displayName();
            commandBuilder.append("#BrowserPage #BrowserContent #FileList", "Pages/BasicTextButton.ui");
            commandBuilder.set("#BrowserPage #BrowserContent #FileList[" + buttonIndex + "].Text", displayText);
            if (!entry.isDirectory()) {
                commandBuilder.set("#BrowserPage #BrowserContent #FileList[" + buttonIndex + "].Style", BUTTON_HIGHLIGHTED);
            }

            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BrowserPage #BrowserContent #FileList[" + buttonIndex + "]",
                    new EventData().append("Action", PageData.Action.BrowserNavigate.name()).append("File", entry.name())
            );
            buttonIndex++;
        }
    }

    private void buildRegularBrowserList(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        String rootDisplayPath = this.getRootDisplayPath(this.browserRoot);
        String currentPath = this.browserCurrent.toString().replace('\\', '/');
        String currentPathDisplay = currentPath.isEmpty() ? rootDisplayPath : rootDisplayPath + "/" + currentPath;
        commandBuilder.set("#BrowserPage #CurrentPath.Text", currentPathDisplay);
        List<File> files = new ObjectArrayList<>();
        Path path = this.browserRoot.resolve(this.browserCurrent);
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path file : stream) {
                    String fileName = file.getFileName().toString();
                    if (fileName.charAt(0) == '/') {
                        fileName = fileName.substring(1);
                    }

                    if ((fileName.endsWith(".prefab.json") || Files.isDirectory(file))
                            && (this.browserSearchQuery.isEmpty() || fileName.toLowerCase().contains(this.browserSearchQuery))) {
                        files.add(file.toFile());
                    }
                }
            } catch (IOException var15) {
                LOGGER.severe("Error reading directory for browser", var15);
            }
        }

        files.sort((a, b) -> {
            if (a.isDirectory() == b.isDirectory()) {
                return a.compareTo(b);
            } else {
                return a.isDirectory() ? -1 : 1;
            }
        });
        int buttonIndex = 0;
        if (!this.browserCurrent.toString().isEmpty() && this.browserSearchQuery.isEmpty()) {
            commandBuilder.append("#BrowserPage #BrowserContent #FileList", "Pages/BasicTextButton.ui");
            commandBuilder.set("#BrowserPage #BrowserContent #FileList[0].Text", "../");
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BrowserPage #BrowserContent #FileList[0]",
                    new EventData().append("Action", PageData.Action.BrowserNavigate.name()).append("File", "..")
            );
            buttonIndex++;
        }

        for (File file : files) {
            boolean isDirectory = file.isDirectory();
            String fileNamex = file.getName();
            commandBuilder.append("#BrowserPage #BrowserContent #FileList", "Pages/BasicTextButton.ui");
            commandBuilder.set("#BrowserPage #BrowserContent #FileList[" + buttonIndex + "].Text", !isDirectory ? fileNamex : fileNamex + "/");
            if (!isDirectory) {
                commandBuilder.set("#BrowserPage #BrowserContent #FileList[" + buttonIndex + "].Style", BUTTON_HIGHLIGHTED);
            }

            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#BrowserPage #BrowserContent #FileList[" + buttonIndex + "]",
                    new EventData().append("Action", PageData.Action.BrowserNavigate.name()).append("File", fileNamex)
            );
            buttonIndex++;
        }
    }

    @Nonnull
    private List<DropdownEntryInfo> buildBrowserRootEntries() {
        List<DropdownEntryInfo> roots = new ObjectArrayList<>();
        roots.add(new DropdownEntryInfo(LocalizableString.fromString("Assets"), "Assets"));
        roots.add(new DropdownEntryInfo(LocalizableString.fromString("Server"), PrefabStore.get().getServerPrefabsPath().toString()));
        return roots;
    }

    @Nullable
    private Path findActualRootPath(@Nonnull String pathStr) {
        for (PrefabStore.AssetPackPrefabPath packPath : PrefabStore.get().getAllAssetPrefabPaths()) {
            if (packPath.prefabsPath().toString().equals(pathStr)) {
                return packPath.prefabsPath();
            }
        }

        if (PrefabStore.get().getServerPrefabsPath().toString().equals(pathStr)) {
            return PrefabStore.get().getServerPrefabsPath();
        } else {
            return PrefabStore.get().getWorldGenPrefabsPath().toString().equals(pathStr) ? PrefabStore.get().getWorldGenPrefabsPath() : null;
        }
    }

    @Nullable
    private AssetPack findAssetPackForPath(@Nonnull String pathStr) {
        Path path = Path.of(pathStr).toAbsolutePath().normalize();

        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path packPrefabsPath = PrefabStore.get().getAssetPrefabsPathForPack(pack).toAbsolutePath().normalize();
            if (path.equals(packPrefabsPath) || path.startsWith(packPrefabsPath)) {
                return pack;
            }
        }

        return null;
    }

    @Nullable
    private PrefabRootDirectory getRootDirectoryForPath(@Nonnull String pathStr) {
        if ("Assets".equals(pathStr)) {
            return PrefabRootDirectory.ASSET;
        } else if (pathStr.equals(PrefabStore.get().getServerPrefabsPath().toString())) {
            return PrefabRootDirectory.SERVER;
        } else if (pathStr.equals(PrefabStore.get().getWorldGenPrefabsPath().toString())) {
            return PrefabRootDirectory.WORLDGEN;
        } else {
            return this.findAssetPackForPath(pathStr) != null ? PrefabRootDirectory.ASSET : null;
        }
    }

    private boolean isAllowedBrowserRoot(@Nonnull String pathStr) {
        return SingleplayerModule.isOwner(this.playerRef) || this.getRootDirectoryForPath(pathStr) != null;
    }

    @Nonnull
    private String getRootDisplayPath(@Nonnull Path root) {
        String rootStr = root.toString();
        if (rootStr.equals(PrefabStore.get().getServerPrefabsPath().toString())) {
            return "ServerRoot/" + root.getFileName();
        } else if (rootStr.equals(PrefabStore.get().getWorldGenPrefabsPath().toString())) {
            Path parent = root.getParent();
            return parent != null && parent.getFileName() != null
                    ? "WorldgenRoot/" + parent.getFileName() + "/" + root.getFileName()
                    : "WorldgenRoot/" + root.getFileName();
        } else {
            AssetPack pack = this.findAssetPackForPath(rootStr);
            if (pack != null) {
                String packPrefix = pack.equals(AssetModule.get().getBaseAssetPack()) ? "HytaleAssets" : "[" + pack.getName() + "]";
                Path parent = root.getParent();
                return parent != null && parent.getFileName() != null
                        ? packPrefix + "/" + parent.getFileName() + "/" + root.getFileName()
                        : packPrefix + "/" + root.getFileName();
            } else {
                return root.toString();
            }
        }
    }


    public static class PageData {
        public static enum Action {
            Generate,
            Cancel,
            TabSelection,
            EntitySelection,
            OpenBrowser,
            BrowserNavigate,
            BrowserRootChanged,
            BrowserSearch,
            ConfirmBrowser,
            CancelBrowser;

            private Action() {
            }
        }

        public static final String ACTION = "Action";
        public static final String TAB = "@Tab";
        public static final String NAME = "@Name";
        public static final String PACK = "@Pack";
        public static final String NETWORK_ID = "NetworkId";
        public static final String CHECKED = "@Checked";
        public static final String CREATE_ITEM = "@CreateItem";
        public static final String BROWSER_FILE = "File";
        public static final String BROWSER_ROOT = "@BrowserRoot";
        public static final String BROWSER_SEARCH = "@BrowserSearch";
        public static final BuilderCodec<SavePage.PageData> CODEC = BuilderCodec.builder(SavePage.PageData.class, SavePage.PageData::new)
                .append(
                        new KeyedCodec<>(ACTION, new EnumCodec<>(PageData.Action.class, EnumCodec.EnumStyle.LEGACY)),
                        (o, action) -> o.action = action,
                        o -> o.action
                )
                .add()
                .append(new KeyedCodec<>(TAB, Codec.STRING), (o, tab) -> o.tab = tab, o -> o.tab)
                .add()
                .append(new KeyedCodec<>(NAME, Codec.STRING), (o, name) -> o.name = name, o -> o.name)
                .add()
                .append(new KeyedCodec<>(PACK, Codec.STRING), (o, pack) -> o.pack = pack, o -> o.pack)
                .add()
                .append(new KeyedCodec<>(NETWORK_ID, Codec.STRING), (o, networkId) -> o.networkId = networkId, o -> o.networkId)
                .add()
                .append(new KeyedCodec<>(CHECKED, Codec.BOOLEAN), (o, checked) -> o.checked = checked, o -> o.checked)
                .add()
                .append(new KeyedCodec<>(CREATE_ITEM, Codec.BOOLEAN), (o, createItem) -> o.createItem = createItem, o -> o.createItem)
                .add()
                .append(new KeyedCodec<>("File", Codec.STRING), (o, browserFile) -> o.browserFile = browserFile, o -> o.browserFile)
                .add()
                .append(new KeyedCodec<>("@BrowserRoot", Codec.STRING), (o, browserRootStr) -> o.browserRootStr = browserRootStr, o -> o.browserRootStr)
                .add()
                .append(new KeyedCodec<>("@BrowserSearch", Codec.STRING), (o, browserSearchStr) -> o.browserSearchStr = browserSearchStr, o -> o.browserSearchStr)
                .add()
                .build();
        public PageData.Action action;
        public String tab;
        public String name;
        public String networkId;
        public String pack;
        public boolean checked;
        public boolean createItem;
        public String browserFile;
        public String browserRootStr;
        public String browserSearchStr;

        public PageData() {
        }
    }
}