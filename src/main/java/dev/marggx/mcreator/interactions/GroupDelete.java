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
import dev.marggx.mcreator.services.HytaleService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.List;

public class GroupDelete extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<GroupDelete> CODEC = BuilderCodec.builder(
            GroupDelete.class, GroupDelete::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
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

        List<Ref<EntityStore>> entities = HytaleService.get().getRefsFromBlockSelection(builderStateSelection, store);
        commandBuffer.run((_store -> {
            for (Ref<EntityStore> entityRef : entities) {
                GroupService.get().leaveGroup(entityRef, _store, null);
            }
            Universe.get().sendMessage(Message.raw("Remove groups done!"));
        }));
    }
}
