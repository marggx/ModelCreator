package dev.marggx.mcomb.commands;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.PrototypePlayerBuilderToolSettings;
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
import dev.marggx.mcomb.data.blockymodel.BlockymodelBase;
import dev.marggx.mcomb.services.BlockymodelService;
import dev.marggx.mcomb.services.MapperService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.nio.file.Path;

public class ModelCombinerCommand extends AbstractCommandCollection {
    public ModelCombinerCommand() {
        super("ptm", "marggx.commands.ptm.desc");
        this.addAliases("ptm");
        this.setPermissionGroup(GameMode.Creative);
        this.addSubCommand(new ModelCombinerCommand.MCombCreate());
        this.requirePermission("hytale.editor.prefab.manage");
    }

    public static class MCombCreate extends AbstractPlayerCommand {
        private final RequiredArg<String> name;

        public MCombCreate() {
            super("create", "marggx.commands.mcomb.desc");
            this.name = this.withRequiredArg("name", "Name for new Model", ArgTypes.STRING);
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

            BlockSelection selection = builderStateSelection.cloneSelection();

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

            BlockymodelBase blockymodel = MapperService.get().createBlockymodelFromBlockSelection(selection);
            Path p = BlockymodelService.get().getBlockymodelPathForPack("Marggx:ModelCombiner");
            BlockymodelService.get().saveBlockymodel(blockymodel, p.resolve("Blocks").resolve(this.name.get(commandContext) + ".blockymodel"));
        }
    }
}