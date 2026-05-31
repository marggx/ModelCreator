package dev.marggx.mcreator.interactions;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.PrototypePlayerBuilderToolSettings;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.services.GroupService;

import javax.annotation.Nonnull;

public class GroupCreate extends SimpleInstantInteraction {

    @Nonnull
    public static final BuilderCodec<GroupCreate> CODEC = BuilderCodec.builder(
            GroupCreate.class, GroupCreate::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        assert playerRef != null;
        Store<EntityStore> store = commandBuffer.getStore();

        if (!PrototypePlayerBuilderToolSettings.isOkayToDoCommandsOnSelection(ref, playerRef, commandBuffer)) {
            return;
        }

        BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(playerComponent, playerRef);
        BlockSelection builderStateSelection = builderState.getSelection();
        if (builderStateSelection == null) {
            return;
        }

        commandBuffer.run(_store -> {
            GroupService.get().autoCreateGroupsBySelection(builderStateSelection, _store);
            Universe.get().sendMessage(Message.raw("Grouping done!"));
        });
    }
}
