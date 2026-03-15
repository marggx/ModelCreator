package dev.marggx.mcreator.ui;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import dev.marggx.mcreator.data.extras.Model;
import dev.marggx.mcreator.services.HytaleService;
import dev.marggx.mcreator.services.MapperService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class SavePage extends InteractiveCustomUIPage<SavePage.PageData> {
    @Nonnull
    private static final Message MESSAGE_SERVER_BUILDER_TOOLS_PREFAB_SAVE_NAME_REQUIRED = Message.translation("server.builderTools.prefabSave.nameRequired");

    private BlockSelection selection;
    private List<Model> models = new ObjectArrayList<>();
    private List<Model> unselectedModels = new ObjectArrayList<>();
    public SavePage(@Nonnull PlayerRef playerRef, BlockSelection selection) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SavePage.PageData.CODEC);
        this.selection = selection;
        this.models = MapperService.get().getBlockymodelsAndEntitiesFromBlockSelection(selection);
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Save.ui");
        uiCommandBuilder.set("#Main #SelectedEntities #Label.Text", Message.translation("mcreator.ui.save.selectedEntitiesLabel").param("count", selection.getEntityCount()));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.SelectedTabChanged, "#Tabs", new EventData().append(PageData.ACTION, PageData.Action.TabSelection.name()).append(PageData.TAB, "#Tabs.SelectedTab"));
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
            case TabSelection:
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

            case Cancel:
                playerComponent.getPageManager().setPage(ref, store, Page.None);
                break;

            case EntitySelection:
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

            case Generate:
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
    }

    public static class PageData {
        public static enum Action {
            Generate,
            Cancel,
            TabSelection,
            EntitySelection;

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
                .build();
        public PageData.Action action;
        public String tab;
        public String name;
        public String networkId;
        public String pack;
        public boolean checked;
        public boolean createItem;

        public PageData() {
        }
    }
}