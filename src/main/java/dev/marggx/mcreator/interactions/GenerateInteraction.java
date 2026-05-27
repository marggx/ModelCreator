package dev.marggx.mcreator.interactions;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.PrototypePlayerBuilderToolSettings;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.ui.SavePage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class GenerateInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<GenerateInteraction> CODEC = BuilderCodec.builder(
            GenerateInteraction.class, GenerateInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler)
    {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        assert playerRef != null;
        Store<EntityStore> store = commandBuffer.getStore();

        if (!PrototypePlayerBuilderToolSettings.isOkayToDoCommandsOnSelection(ref, playerRef, commandBuffer)) {
            playerComponent.getPageManager().openCustomPage(ref, store, new SavePage(playerRef, null));
            return;
        }

        BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(playerComponent, playerRef);
        BlockSelection builderStateSelection = builderState.getSelection();
        if (builderStateSelection == null) {
            playerComponent.getPageManager().openCustomPage(ref, store, new SavePage(playerRef, null));
            return;
        }

        BlockSelection selection = new BlockSelection();
        selection.setSelectionArea(builderStateSelection.getSelectionMin(), builderStateSelection.getSelectionMax());

        int xMin = selection.getSelectionMin().x();
        int yMin = selection.getSelectionMin().y();
        int zMin = selection.getSelectionMin().z();
        int width = selection.getSelectionMax().x() - xMin;
        int height = selection.getSelectionMax().y() - yMin;
        int depth = selection.getSelectionMax().z() - zMin;
        selection.setPosition(xMin + width/2, yMin, zMin + depth/2);
        BuilderToolsPlugin.forEachCopyableInSelection(commandBuffer.getExternalData().getWorld(), xMin, yMin, zMin, width, height, depth, e -> {
            Holder<EntityStore> holder = store.copyEntity(e);
            selection.addEntityFromWorld(holder);
        });

        playerComponent.getPageManager().openCustomPage(ref, store, new SavePage(playerRef, selection));
    }
}
