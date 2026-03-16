package dev.marggx.mcreator;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.marggx.mcreator.commands.ModelCreatorCommand;

import javax.annotation.Nonnull;

public class ModelCreatorPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ModelCreatorPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Helloa from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new ModelCreatorCommand());
    }

    @Override
    protected void start() {
    }
}