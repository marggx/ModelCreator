package dev.marggx.mcreator.services;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.prefablist.AssetPrefabFileProvider;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.ModelCreatorPlugin;
import dev.marggx.mcreator.data.blockymodel.BlockymodelVector3d;
import dev.marggx.mcreator.data.extras.BaseModel;
import dev.marggx.mcreator.data.extras.Model;
import dev.marggx.mcreator.utils.Logger;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HytaleService {

    public static final int NODE_LIMIT = 255;
    private static final HytaleService INSTANCE = new HytaleService();
    private static final Logger LOGGER = Logger.get();
    private final AssetPrefabFileProvider assetProvider = new AssetPrefabFileProvider();

    public static HytaleService get() {
        return INSTANCE;
    }

    public List<Holder<EntityStore>> getEntitiesFromPrefab(String prefabPath) {
        Path path = PrefabStore.get().findAssetPrefabPath(prefabPath);
        if (path == null) {
            LOGGER.severe("Prefab not found: " + prefabPath);
            return null;
        }

        BlockSelection prefab = PrefabStore.get().getPrefab(path);
        return getHoldersFromBlockSelectionForModel(prefab);
    }

    public BlockSelection getBlockSelectionFromPrefab(String prefabPath) {
        Path path = this.assetProvider.resolveVirtualPath(prefabPath);
        path = path != null ? path : PrefabStore.get().findAssetPrefabPath(prefabPath);
        BlockSelection prefab = null;
        if (path == null) {
            try {
                prefab = PrefabStore.get().getServerPrefab(prefabPath);
            } catch (Exception e) {
                LOGGER.severe("Failed to load prefab from path '%s': %s", prefabPath, e.getMessage());
            }
        } else {
            try {
                prefab = PrefabStore.get().getPrefab(path);
            } catch (Exception e) {
                LOGGER.severe("Failed to load prefab from path '%s': %s", prefabPath, e.getMessage());
            }
        }
        return prefab;
    }

    public BlockSelection cloneBlockSelectionWithEntitiesInSelection(BlockSelection selection, Store<EntityStore> store) {
        BlockSelection clonedSelection = new BlockSelection();

        Vector3i min = selection.getSelectionMin();
        Vector3i max = selection.getSelectionMax();
        clonedSelection.setSelectionArea(min, max);

        int xMin = min.x();
        int yMin = min.y();
        int zMin = min.z();
        int width = max.x() - xMin;
        int height = max.y() - yMin;
        int depth = max.z() - zMin;
        clonedSelection.setPosition(xMin + width / 2, yMin, zMin + depth / 2);
        BuilderToolsPlugin.forEachCopyableInSelection(store.getExternalData().getWorld(), xMin, yMin, zMin, width, height, depth, e -> {
            Holder<EntityStore> holder = store.copyEntity(e);
            clonedSelection.addEntityFromWorld(holder);
        });

        return clonedSelection;
    }

    public List<Ref<EntityStore>> getRefsFromBlockSelection(BlockSelection selection, Store<EntityStore> store) {
        Vector3i min = selection.getSelectionMin();
        Vector3i max = selection.getSelectionMax();
        int width = max.x() - min.x();
        int height = max.y() - min.y();
        int depth = max.z() - min.z();
        List<Ref<EntityStore>> entities = new ReferenceArrayList<>();
        BuilderToolsPlugin.forEachCopyableInSelection(store.getExternalData().getWorld(), min.x(), min.y(), min.z(), width, height, depth, entities::add);
        return entities;
    }

    public List<Holder<EntityStore>> getHoldersFromBlockSelectionForModel(BlockSelection selection) {
        List<Holder<EntityStore>> entities = new ObjectArrayList<>();
        selection.forEachEntity(e -> entities.add(e.clone()));
        return entities;
    }

    public List<Model> deduplicateModels(List<Model> models) {
        Map<String, String> modelCache = new HashMap<>();
        List<Model> dedupedModels = new ObjectArrayList<>();
        for (Model model : models) {
            Holder<EntityStore> holder = model.holder();
            TransformComponent transform = holder.getComponent(TransformComponent.getComponentType());
            if (transform == null) return null;

            BlockymodelVector3d position = BlockymodelVector3d.from(transform.getPosition());
            HeadRotation headRotation = holder.getComponent(HeadRotation.getComponentType());
            Rotation3f rotationVector = MapperService.get().createRotationVector(model, headRotation, transform);
            String cacheKey = model.id() + position + rotationVector;
            if (modelCache.containsKey(cacheKey)) {
                continue;
            }
            modelCache.put(cacheKey, model.id());
            dedupedModels.add(model);
        }
        return dedupedModels;
    }


    public void createNewItem(BaseModel baseModel, Consumer<Item> onLoaded) throws IOException {
        String content = """
                {
                  "TranslationProperties": {
                    "Name": "server.items.<Name>.name"
                  },
                  "Icon": "Icons/ItemsGenerated/Debug_Block.png",
                  "Categories": [],
                  "Quality": "Developer",
                  "Interactions": {
                    "Primary": "Block_Primary",
                    "Secondary": "Block_Secondary"
                  },
                  "BlockType": {
                    "Material": "Solid",
                    "DrawType": "Model",
                    "CustomModel": "<ModelPath>",
                    "CustomModelTexture": [
                      {
                        "Texture": "<TexturePath>",
                        "Weight": 1
                      }
                    ],
                    "ParticleColor": "#0000f4"
                  },
                  "PlayerAnimationsId": "Block",
                  "Tags": {
                    "Type": [
                      "Debug"
                    ]
                  }
                }
                """;

        String name = createValidItemName(baseModel.name());

        content = content
                .replace("<Name>", name)
                .replace("<ModelPath>", "Blocks/" + baseModel.name() + ".blockymodel")
                .replace("<TexturePath>", "Blocks/" + baseModel.name() + ".png");

        Path outputPath = null;
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (!pack.getName().equals(baseModel.pack())) continue;
            outputPath = pack.getRoot().resolve("Server").resolve("Item").resolve("Items");
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            outputPath = outputPath.resolve(name + ".json");
        }
        if (outputPath == null) return;
        Files.writeString(outputPath, content);
        if (onLoaded == null) return;
        ModelCreatorPlugin.get().pendingItems.put(name, onLoaded);
    }

    public void replaceEntitiesWithNewItem(Vector3d pos, String modelId, BlockSelection selection, Store<EntityStore> store) {
        this.removeEntitiesInSelection(selection, store);
        this.placeNewItem(pos, modelId, store);
    }

    public List<Holder<EntityStore>> removeEntitiesInSelection(BlockSelection selection, Store<EntityStore> store) {
        List<Ref<EntityStore>> refs = this.getRefsFromBlockSelection(selection, store);
        List<Holder<EntityStore>> holders = new ObjectArrayList<>();
        for (Ref<EntityStore> ref : refs) {
            holders.add(store.removeEntity(ref, RemoveReason.REMOVE));
        }
        return holders;
    }

    public Ref<EntityStore> placeNewItem(Vector3d pos, String modelId, Store<EntityStore> store) {
        Holder<EntityStore> holder = store.getRegistry().newHolder();
        holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(modelId));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(pos, new Rotation3f()));
        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(2.0F));
        ItemStack itemStack = new ItemStack(modelId, 1);
        itemStack.setOverrideDroppedItemAnimation(true);
        holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(itemStack));
        holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
        holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
        holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
        holder.ensureComponent(UUIDComponent.getComponentType());
        return store.addEntity(holder, AddReason.SPAWN);
    }

    public String createValidItemName(String name) {
        if (Character.isDigit(name.charAt(0))) {
            return "Item_" + name;
        }

        if (Character.isUpperCase(name.charAt(0))) {
            return name;
        }

        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
