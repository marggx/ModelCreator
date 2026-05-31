package dev.marggx.mcreator.commands;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.PrototypePlayerBuilderToolSettings;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.services.HytaleService;
import dev.marggx.mcreator.ui.SavePage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ModelCreatorCommand extends AbstractCommandCollection {
    public ModelCreatorCommand() {
        super("mcreator", "mcreator.command.desc");
        this.addAliases("mc");
        this.setPermissionGroups("hytale:Builder");
        this.addSubCommand(new ModelCreatorCommand.MCreatorCreate());
        this.requirePermission("hytale.editor.prefab.manage");
    }

    public static class MCreatorCreate extends AbstractPlayerCommand {
        public MCreatorCreate() {
            super("generate", "mcreator.command.generate.desc");
            this.addAliases("g");
        }

        @Override
        protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            assert playerComponent != null;

            if (!PrototypePlayerBuilderToolSettings.isOkayToDoCommandsOnSelection(ref, playerRef, store)) {
                playerComponent.getPageManager().openCustomPage(ref, store, new SavePage(playerRef, null));
                return;
            }

            BuilderToolsPlugin.BuilderState builderState = BuilderToolsPlugin.getState(playerComponent, playerRef);
            BlockSelection builderStateSelection = builderState.getSelection();
            if (builderStateSelection == null) {
                playerComponent.getPageManager().openCustomPage(ref, store, new SavePage(playerRef, null));
                return;
            }

            BlockSelection selection = HytaleService.get().cloneBlockSelectionWithEntitiesInSelection(builderStateSelection, store);

            playerComponent.getPageManager().openCustomPage(ref, store, new SavePage(playerRef, selection));
        }
    }
}