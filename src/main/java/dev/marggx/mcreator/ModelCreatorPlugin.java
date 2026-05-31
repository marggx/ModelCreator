package dev.marggx.mcreator;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.io.ServerManager;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.commands.ModelCreatorCommand;
import dev.marggx.mcreator.components.GroupComponent;
import dev.marggx.mcreator.components.GroupMembershipComponent;
import dev.marggx.mcreator.interactions.GenerateInteraction;
import dev.marggx.mcreator.interactions.GroupCreate;
import dev.marggx.mcreator.interactions.GroupDelete;
import dev.marggx.mcreator.interactions.GroupToggle;
import dev.marggx.mcreator.systems.GroupSystems;
import dev.marggx.mcreator.ui.EditPage;
import dev.marggx.mcreator.ui.supplier.EditPageSupplier;
import dev.marggx.mcreator.utils.Logger;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ModelCreatorPlugin extends JavaPlugin {
    private static ModelCreatorPlugin INSTANCE;
    private ComponentType<EntityStore, GroupComponent> groupComponentType;
    private ComponentType<EntityStore, GroupMembershipComponent> groupMembershipComponentType;
    public final ConcurrentHashMap<String, Consumer<Item>> pendingItems = new ConcurrentHashMap<>();

    public static ModelCreatorPlugin get() {
        return INSTANCE;
    }

    public ModelCreatorPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
        Logger.get().info("Plugin " + this.getName() + " with version " + this.getManifest().getVersion().toString() + " is starting");
    }

    @Override
    protected void setup() {
        Logger.get().info("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new ModelCreatorCommand());

        OpenCustomUIInteraction.registerCustomPageSupplier(this, EditPage.class, "EditPage", new EditPageSupplier());
        this.groupMembershipComponentType = this.getEntityStoreRegistry().registerComponent(GroupMembershipComponent.class, "GroupMembershipComponent", GroupMembershipComponent.CODEC);
        this.groupComponentType = this.getEntityStoreRegistry().registerComponent(GroupComponent.class, () -> {
            throw new UnsupportedOperationException("Not implemented");
        });

        this.getCodecRegistry(Interaction.CODEC).register("MCreatorGenerate", GenerateInteraction.class, GenerateInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("GroupCreate", GroupCreate.class, GroupCreate.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("GroupDelete", GroupDelete.class, GroupDelete.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("GroupToggle", GroupToggle.class, GroupToggle.CODEC);

        this.getEntityStoreRegistry().registerSystem(new GroupSystems.GroupEntityRemoved());
        this.getEntityStoreRegistry().registerSystem(new GroupSystems.GroupMemberEntityRef(this.groupMembershipComponentType));
        this.getEventRegistry().register(LoadedAssetsEvent.class, Item.class, this::onItemLoaded);
        ServerManager.get().registerSubPacketHandlers(dev.marggx.mcreator.services.PacketHandler::new);
    }

    @Override
    protected void start() {
    }

    public void onItemLoaded(@Nonnull LoadedAssetsEvent<String, Item, ?> event) {
        for (Item item : event.getLoadedAssets().values()) {
            Consumer<Item> callback = pendingItems.remove(item.getId());
            if (callback != null) {
                callback.accept(item);
            }
        }
    }

    public ComponentType<EntityStore, GroupComponent> getGroupComponentType() {
        return this.groupComponentType;
    }

    public ComponentType<EntityStore, GroupMembershipComponent> getGroupMembershipComponentType() {
        return this.groupMembershipComponentType;
    }
}