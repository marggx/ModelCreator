package dev.marggx.mcomb.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.file.Path;
import java.util.List;

public class PrefabService {

    private static final PrefabService INSTANCE = new PrefabService();
    public static PrefabService get() {
        return INSTANCE;
    }

    public List<Holder<EntityStore>> getEntitiesFromPrefab(String prefabPath) {
        Path path = PrefabStore.get().findAssetPrefabPath(prefabPath);
        if (path == null) return null;

        BlockSelection prefab = PrefabStore.get().getPrefab(path);
        return getEntitiesFromBlockSelection(prefab);
    }

    public List<Holder<EntityStore>> getEntitiesFromBlockSelection(BlockSelection selection) {
        int xMin = selection.getSelectionMin().getX();
        int zMin = selection.getSelectionMin().getZ();
        int width = selection.getSelectionMax().getX() - xMin;
        int depth = selection.getSelectionMax().getZ() - zMin;
        List<Holder<EntityStore>> entities = new ObjectArrayList<>();
        selection.forEachEntity(e -> entities.add(e.clone()));
        entities.forEach((e) -> {
            TransformComponent pos = e.getComponent(TransformComponent.getComponentType());
            if (pos == null) return;
            pos.getPosition().add((double) width, 0.0, (double) depth);
        });
        return entities;
    }
}
