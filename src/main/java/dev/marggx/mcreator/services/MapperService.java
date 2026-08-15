package dev.marggx.mcreator.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.util.TrigMathUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.data.blockymodel.*;
import dev.marggx.mcreator.data.extras.BaseModel;
import dev.marggx.mcreator.data.extras.Model;
import dev.marggx.mcreator.utils.Logger;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MapperService {
    private static final MapperService INSTANCE = new MapperService();
    private static final Logger LOGGER = Logger.get();

    public static MapperService get() {
        return INSTANCE;
    }

    private final BlockymodelService blockymodelService = BlockymodelService.get();
    private final TextureService textureService = TextureService.get();
    private final HytaleService hytaleService = HytaleService.get();

    public boolean createBlockymodelFromPrefab(Path prefabPath, String pack, String name, boolean createNewItem) {
        BlockSelection prefab = PrefabStore.get().getPrefab(prefabPath);
        List<Model> blockymodels = getBlockymodelsAndEntitiesFromBlockSelection(prefab);
        if (blockymodels == null) return false;

        Vector3d position = new Vector3d(prefab.getX(), prefab.getY(), prefab.getZ());
        return createBlockymodel(blockymodels, position, null, pack, name, createNewItem, null);
    }

    public boolean createBlockymodelFromBlockSelection(BlockSelection selection, String pack, String name, boolean createNewItem) {
        List<Model> blockymodels = getBlockymodelsAndEntitiesFromBlockSelection(selection);
        if (blockymodels == null) return false;

        Vector3d position = new Vector3d(selection.getX(), selection.getY(), selection.getZ());
        return createBlockymodel(blockymodels, position, null, pack, name, createNewItem, null);
    }

    public boolean createBlockymodel(List<Model> blockymodels, Vector3d position, Box hitbox, String pack, String name, boolean createNewItem, Consumer<Item> onLoaded) {
        BaseModel base = new BaseModel(600, position, null, null, null, null, hitbox);
        BaseModel model = createBlockymodel(blockymodels, base);

        model.setName(name);
        model.setPack(pack);
        boolean created = createNewModel(model);
        textureService.clearCache();
        if (!created) return false;

        if (!createNewItem) return true;
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            try {
                hytaleService.createNewItem(base, onLoaded);
            } catch (IOException e) {
                LOGGER.severe("Failed to create new item for blockymodel: " + model.name(), e);
            }
        }, 10L, TimeUnit.SECONDS);
        return true;
    }

    private boolean createNewModel(BaseModel model) {
        boolean valid = model.validate();
        if (!valid) return false;

        if (model.hitBox() != null) {
            try {
                hytaleService.createNewHitbox(model);
            } catch (IOException e) {
                Logger.get().severe("Could not create Hitbox for model: " + model.name());
            }
        }
        return blockymodelService.saveBlockymodelBase(model) && textureService.saveTexture(model);
    }

    private BaseModel createBlockymodel(List<Model> models, BaseModel base) {
        for (Model existingModel : models) {
            Blockymodel blockymodel = createBlockymodelFromExistingModel(base, existingModel);
            if (blockymodel == null) continue;

            base.incrementBlockyId();
            base.addBlockymodel(blockymodel);
        }

        return base;
    }

    private Blockymodel createBlockymodelFromExistingModel(BaseModel base, Model model) {
        Holder<EntityStore> holder = model.holder();
        TransformComponent transform = holder.getComponent(TransformComponent.getComponentType());
        if (transform == null) {
            LOGGER.warning("Model " + model.id() + " has no transform component. Cannot create blockymodel.");
            return null;
        }

        BlockymodelVector3d position = BlockymodelVector3d.from(transform.getPosition());
        HeadRotation headRotation = holder.getComponent(HeadRotation.getComponentType());
        Vector3d rotationVector = createRotationVector(model, headRotation, transform);

        BlockymodelBase blockymodelBase = blockymodelService.loadBlockymodelBase(model.path());
        if (blockymodelBase == null) {
            LOGGER.severe("Model " + model.id() + " has no blockymodel. Cannot create blockymodel.");
            return null;
        }
        model.setBlockymodel(blockymodelBase);

        Quaterniond orientation = BlockymodelQuaternion.fromVector3d(rotationVector);
        textureService.handleTexture(model, base);

        if (model.attachedModels() != null) {
            blockymodelService.addAttachments(model, base);
        }


        handleHeadRotation(orientation, model, holder, blockymodelBase);

        BlockymodelVector3d offset;
        offset = BlockymodelVector3d.from(new Vector3d(0, -16.0, 0));
        //boolean hasTransformRotation = hasTransformRotation(model, headRotation, transform);
        handlePosition(base, position);
        handleScale(base, model, holder, blockymodelBase, offset, position);

        BlockymodelShape shape = new BlockymodelShape(
                offset,
                new BlockymodelVector3d(),
                new HashMap<>(),
                BlockymodelShapeType.None,
                new BlockymodelShapeSettings()
        );

        return new Blockymodel(
                base.getStrBlockyId(),
                model.id(),
                BlockymodelVector3d.from(position),
                new BlockymodelQuaternion(orientation),
                shape,
                blockymodelBase.getNodes()
        );
    }

    public List<Model> getBlockymodelsAndEntitiesFromPrefab(String prefabName) {
        List<Holder<EntityStore>> entities = hytaleService.getEntitiesFromPrefab(prefabName);
        if (entities.isEmpty()) {
            LOGGER.warning("No entities found in prefab. Cannot create List<Model>.");
            return null;
        }
        return getModelsFromEntities(entities);
    }

    public List<Model> getBlockymodelsAndEntitiesFromBlockSelection(BlockSelection selection) {
        List<Holder<EntityStore>> entities = hytaleService.getHoldersFromBlockSelectionForModel(selection);
        if (entities.isEmpty()) {
            LOGGER.warning("No entities found in selection. Cannot create List<Model>.");
            return null;
        }
        return getModelsFromEntities(entities);
    }

    public List<Model> getModelsFromEntities(List<Holder<EntityStore>> entities) {
        List<Model> list = new ObjectArrayList<>();
        for (Holder<EntityStore> entity : entities) {
            Model model = blockymodelService.loadModelFromHolder(entity);

            if (model == null || !model.validate()) {
                LOGGER.severe("Something went wrong when loading a model from an entity. " + model);
                continue;
            }

            list.add(model);
        }
        return hytaleService.deduplicateModels(list);
    }

    public Vector3d createRotationVector(Model model, HeadRotation headRotation, TransformComponent transform) {
        Vector3d rot;
        if (headRotation == null) {
            rot = new Vector3d(transform.getRotation().x(), transform.getRotation().y(), transform.getRotation().z());
        } else if (hasTransformRotation(model, headRotation, transform)) {
            rot = new Vector3d(transform.getRotation().x(), transform.getRotation().y(), transform.getRotation().z());
        } else {
            rot = new Vector3d(headRotation.getRotation().x(), headRotation.getRotation().y(), headRotation.getRotation().z());
        }

        rot.rotateY(TrigMathUtil.PI);
        return rot;
    }

    private boolean hasTransformRotation(Model model, HeadRotation headRotation, TransformComponent transform) {
        if (model.getType() == Model.ModelType.MODEL) return true;
        if (model.getType() == Model.ModelType.BLOCK) return true;
        return headRotation.getRotation().x() == 0.0f && headRotation.getRotation().z() == 0.0f && (transform.getRotation().x() != 0.0f || transform.getRotation().z() != 0.0f);
    }

    private void handlePosition(BaseModel base, BlockymodelVector3d position) {
        position.sub(base.position());

        position.x = -position.x();
        position.z = -position.z();

        //One Hytale unit = 32 Blockbench units
        position.mul(32.0);
        position.round(6);
    }

    private void handleScale(BaseModel base, Model model, Holder<EntityStore> holder, BlockymodelBase blockymodelBase, BlockymodelVector3d offset, BlockymodelVector3d pos) {
        EntityScaleComponent scaleComponent = holder.getComponent(EntityScaleComponent.getComponentType());
        double scale = scaleComponent == null ? 1.0 : scaleComponent.getScale();
        if (model.getType() == Model.ModelType.MODEL) {
            ModelComponent modelComponent = holder.getComponent(ModelComponent.getComponentType());
            assert modelComponent != null;
            scale = modelComponent.getModel().getScale() / 2;
            offset.y = 0;
        } else if (model.getType() == Model.ModelType.ITEM) {
            scale /= 2.5;
            offset.y = 0;
        }
        offset.mul(scale);
        blockymodelService.scaleBlockymodel(blockymodelBase, scale);
    }

    private void handleHeadRotation(Quaterniond baseOrientation, Model model, Holder<EntityStore> holder, BlockymodelBase blockymodelBase) {
        if (model.getType() != Model.ModelType.MODEL) {
            return;
        }

        HeadRotation headRotation = holder.getComponent(HeadRotation.getComponentType());
        if (headRotation == null) {
            return;
        }

        BlockymodelQuaternion orientation = BlockymodelQuaternion.getLocalQuat(baseOrientation, headRotation.getRotation());
        boolean didWork = blockymodelService.setHeadRotation(blockymodelBase, orientation);
        if (!didWork) {
            LOGGER.severe("Failed to set head rotation for model: " + model.id());
        }
    }
}
