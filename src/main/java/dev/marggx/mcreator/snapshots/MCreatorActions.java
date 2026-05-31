package dev.marggx.mcreator.snapshots;


import com.hypixel.hytale.builtin.buildertools.UndoAction;
import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;

public enum MCreatorActions implements UndoAction {
    BLOCKY_REPLACEMENT_SNAPSHOT("mcreator.builderTools.action.blocky.replacement");

    private final String translationKey;

    MCreatorActions(String translationKey) {
        this.translationKey = translationKey;
    }

    @Nonnull
    public String id() {
        return "mcreator:" + this.name().toLowerCase();
    }

    @Nonnull
    public Message toMessage() {
        return Message.translation(this.translationKey);
    }
}