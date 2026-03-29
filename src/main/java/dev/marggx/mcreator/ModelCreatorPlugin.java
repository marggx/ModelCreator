package dev.marggx.mcreator;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.marggx.mcreator.commands.ModelCreatorCommand;
import dev.marggx.mcreator.ui.EditPage;
import dev.marggx.mcreator.ui.supplier.EditPageSupplier;
import dev.marggx.mcreator.utils.Logger;

import javax.annotation.Nonnull;

public class ModelCreatorPlugin extends JavaPlugin {

    public ModelCreatorPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        Logger.get().info("Plugin " + this.getName() + " with version " + this.getManifest().getVersion().toString() + " is starting");
    }

    @Override
    protected void setup() {
        Logger.get().info("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new ModelCreatorCommand());

        OpenCustomUIInteraction.registerCustomPageSupplier(this, EditPage.class, "EditPage", new EditPageSupplier());
    }

    @Override
    protected void start() {
    }
}