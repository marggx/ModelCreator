package dev.marggx.mcomb.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcomb.data.blockymodel.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.HashMap;
import java.util.List;

public class MapperService {
    private static final MapperService INSTANCE = new MapperService();
    public static MapperService get() {
        return INSTANCE;
    }

    public BlockymodelBase createBlockymodelFromPrefab(String prefabName) {
        List<BtH> blockymodels = getBlockymodelsAndEntitiesFromPrefab(prefabName);
        if (blockymodels == null) return null;

        return createBlockymodel(blockymodels);
    }

    public BlockymodelBase createBlockymodelFromBlockSelection(BlockSelection selection) {
        List<BtH> blockymodels = getBlockymodelsAndEntitiesFromBlockSelection(selection);
        if (blockymodels == null) return null;

        return createBlockymodel(blockymodels);
    }

    private BlockymodelBase createBlockymodel(List<BtH> prefabBlockymodels) {
        List<Blockymodel> blockymodels = new ObjectArrayList<>();
        int id = 600;
        for (BtH bth : prefabBlockymodels) {
            Blockymodel model = createBlockymodelFromBth(String.valueOf(id), bth);
            if (model == null) continue;
            blockymodels.add(model);
            id++;
        }
        Blockymodel[] nodes = blockymodels.toArray(Blockymodel[]::new);
        return new BlockymodelBase(null, nodes);
    }

    private Blockymodel createBlockymodelFromBth(String id, BtH bth) {
        Holder<EntityStore> holder = bth.holder();
        TransformComponent transform = holder.getComponent(TransformComponent.getComponentType());
        if (transform == null) return null;

        BlockymodelVector3d pos = BlockymodelVector3d.from(transform.getPosition());
        // One Hytale unit = 32 Blockbench units
        pos.setX(-pos.getX());
        pos.setZ(-pos.getZ());
        pos.scale(32.0);
        pos.add(32.0, 0.0, 32.0);
        HeadRotation headRotation = holder.getComponent(HeadRotation.getComponentType());
        if (headRotation == null) return null;

        BlockymodelQuaternion orientation = BlockymodelQuaternion.fromVector3f(headRotation.getRotation());
        EntityScaleComponent scale = holder.getComponent(EntityScaleComponent.getComponentType());
        if (scale == null) return null;

        BlockymodelBase blockymodelBase = bth.blockymodel();
        if (blockymodelBase == null) return null;

        BlockymodelService.get().scaleBlockymodel(blockymodelBase, scale.getScale() / 2);
        BlockymodelVector3d offset = BlockymodelVector3d.from(new Vector3d(0, -16.0, 0));

        BlockymodelShape shape = new BlockymodelShape(
                offset,
                new BlockymodelVector3d(),
                new HashMap<>(),
                BlockymodelShapeType.None,
                new BlockymodelShapeSettings()
        );

        return new Blockymodel(
                id,
                BlockymodelService.get().getHolderId(holder),
                BlockymodelVector3d.from(pos),
                orientation,
                shape,
                blockymodelBase.getNodes()
        );
    }

    private List<BtH> getBlockymodelsAndEntitiesFromPrefab(String prefabName) {
        List<Holder<EntityStore>> entities = PrefabService.get().getEntitiesFromPrefab(prefabName);
        if (entities.isEmpty()) return null;
        return getBlockymodelsFromEntities(entities);
    }

    private List<BtH> getBlockymodelsAndEntitiesFromBlockSelection(BlockSelection selection) {
        List<Holder<EntityStore>> entities = PrefabService.get().getEntitiesFromBlockSelection(selection);
        if (entities.isEmpty()) return null;
        return getBlockymodelsFromEntities(entities);
    }

    private List<BtH> getBlockymodelsFromEntities(List<Holder<EntityStore>> entities) {
        List<BtH> list = new ObjectArrayList<>();
        for (Holder<EntityStore> entity : entities) {
            BlockymodelBase blockymodel = BlockymodelService.get().loadBlockymodel(entity);

            if (blockymodel != null) list.add(new BtH(blockymodel, entity));
        }
        return list;
    }

    private record BtH(BlockymodelBase blockymodel, Holder<EntityStore> holder) {}
}
