package dev.marggx.mcreator.snapshots;

import com.hypixel.hytale.builtin.buildertools.snapshot.SelectionSnapshot;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BlockyReplacementSnapshot implements SelectionSnapshot<BlockyReplacementSnapshot> {
    public List<Holder<EntityStore>> holders;
    public ConcurrentHashMap<String, Ref<EntityStore>> refs = new ConcurrentHashMap<>();

    public BlockyReplacementSnapshot(List<Holder<EntityStore>> holders, List<Ref<EntityStore>> refs) {
        this.holders = holders;
        if (refs == null) {
            return;
        }
        int counter = 0;
        for (Ref<EntityStore> ref : refs) {
            this.refs.put(String.valueOf(counter), ref);
            counter++;
        }
    }

    public List<Holder<EntityStore>> getHolders() {
        return this.holders;
    }

    public BlockyReplacementSnapshot restore(Ref<EntityStore> ref, PlayerRef playerRef, @Nonnull World world, ComponentAccessor<EntityStore> componentAccessor) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        List<Ref<EntityStore>> newRefs = new ObjectArrayList<>();
        List<Holder<EntityStore>> newHolders = new ObjectArrayList<>();
        for (Holder<EntityStore> holder : this.holders) {
            newRefs.add(store.addEntity(holder, AddReason.SPAWN));
        }

        for (Ref<EntityStore> createdRef : this.refs.values()) {
            newHolders.add(store.removeEntity(createdRef, RemoveReason.BUILDER_TOOLS_UNDO));
        }
        return new BlockyReplacementSnapshot(newHolders, newRefs);
    }

}

