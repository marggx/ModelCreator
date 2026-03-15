package dev.marggx.mcreator.commands;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.PrototypePlayerBuilderToolSettings;
import com.hypixel.hytale.builtin.buildertools.prefablist.PrefabSavePage;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.services.BlockymodelService;
import dev.marggx.mcreator.services.MapperService;
import dev.marggx.mcreator.ui.SavePage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ModelCreatorCommand extends AbstractCommandCollection {
    public ModelCreatorCommand() {
        super("mcreator", "mcreator.command.desc");
        this.addAliases("mc");
        this.setPermissionGroup(GameMode.Creative);
        this.addSubCommand(new ModelCreatorCommand.MCreatorCreate());
        this.requirePermission("hytale.editor.prefab.manage");
    }

    public static class MCreatorCreate extends AbstractPlayerCommand {
        public MCreatorCreate() {
            super("generate", "mcreator.command.desc");
            this.addAliases("g");
        }

        @Override
        protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world)
        {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            assert playerComponent != null;

            if (!PrototypePlayerBuilderToolSettings.isOkayToDoCommandsOnSelection(ref, playerComponent, store)) {
                return;
            }

            BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(playerComponent, playerRef);
            BlockSelection builderStateSelection = builderState.getSelection();
            if (builderStateSelection == null) {
                playerRef.sendMessage(Message.translation("server.commands.clear.noSelection"));
                return;
            }

            BlockSelection selection = new BlockSelection();
            selection.setSelectionArea(builderStateSelection.getSelectionMin(), builderStateSelection.getSelectionMax());

            int xMin = selection.getSelectionMin().getX();
            int yMin = selection.getSelectionMin().getY();
            int zMin = selection.getSelectionMin().getZ();
            int width = selection.getSelectionMax().getX() - xMin;
            int height = selection.getSelectionMax().getY() - yMin;
            int depth = selection.getSelectionMax().getZ() - zMin;
            selection.setPosition(xMin + width/2, yMin, zMin + depth/2);
            BuilderToolsPlugin.forEachCopyableInSelection(world, xMin, yMin, zMin, width, height, depth, e -> {
                Holder<EntityStore> holder = store.copyEntity(e);
                selection.addEntityFromWorld(holder);
            });

            playerComponent.getPageManager().openCustomPage(ref, store, new SavePage(playerRef, selection));
        }
    }
}