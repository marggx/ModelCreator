package dev.marggx.mcreator.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.ModelCreatorPlugin;
import dev.marggx.mcreator.utils.Logger;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class GroupComponent implements Component<EntityStore> {
    @Nonnull
    private final List<Ref<EntityStore>> memberList = new ReferenceArrayList();

    public static ComponentType<EntityStore, GroupComponent> getComponentType() {
        return ModelCreatorPlugin.get().getGroupComponentType();
    }

    public void forEachMember(@Nonnull Consumer<Ref<EntityStore>> consumer, Ref<EntityStore> excludeReference) {
        for (Ref<EntityStore> member : this.memberList) {
            if (member.isValid() && !member.equals(excludeReference)) {
                consumer.accept(member);
            }
        }
    }

    public void add(@Nonnull Ref<EntityStore> reference) {
        try {
            this.memberList.add(reference);
        } catch (Exception e) {
            Logger.get().severe(e.getMessage());
        }
    }

    public void remove(@Nonnull Ref<EntityStore> reference) {
        try {
            this.memberList.remove(reference);
        } catch (Exception e) {
            Logger.get().severe(e.getMessage());
        }
    }

    public boolean contains(@Nonnull Ref<EntityStore> reference) {
        try {
            return this.memberList.contains(reference);
        } catch (Exception e) {
            Logger.get().severe(e.getMessage());
        }
        return false;
    }

    @Nullable
    public Ref<EntityStore> getFirst() {
        return !this.memberList.isEmpty() ? this.memberList.getFirst() : null;
    }

    @Nonnull
    public List<Ref<EntityStore>> getMemberList() {
        return this.memberList;
    }

    public int size() {
        return this.memberList.size();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new GroupComponent();
    }
}
