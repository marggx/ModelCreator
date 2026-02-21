package dev.marggx.mcomb.services;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.BsonUtil;
import dev.marggx.mcomb.data.blockymodel.Blockymodel;
import dev.marggx.mcomb.data.blockymodel.BlockymodelBase;
import dev.marggx.mcomb.data.blockymodel.BlockymodelVector3d;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlockymodelService {

    private static final BlockymodelService INSTANCE = new BlockymodelService();
    public static BlockymodelService get() {
        return INSTANCE;
    }

    public BlockymodelBase loadBlockymodel(String path) {
        Path p = getBlockymodelPathFromAnyPack(path);
        if (p == null) return null;
        return load(p);
    }

    public BlockymodelBase loadBlockymodel(Holder<EntityStore> holder) {
        String id = getHolderId(holder);
        if (id == null) return null;
        String path = getItemModelId(id);
        return loadBlockymodel(path);
    }

    public String getHolderId(Holder<EntityStore> holder) {
        ItemComponent itemComp = holder.getComponent(ItemComponent.getComponentType());

        if (itemComp != null) {
            ItemStack itemStack = itemComp.getItemStack();
            if (itemStack != null) {
                Item item = getItemFromAssetsById(itemStack.getItemId());
                if (item != null) {
                    return item.getId();
                }
            }
        }

        BlockEntity blockEntity = holder.getComponent(BlockEntity.getComponentType());
        if (blockEntity == null) return null;
        return blockEntity.getBlockTypeKey();
    }

    public BlockymodelBase loadBlockymodel(Item item) {
        String modelId = getItemModelId(item);
        if (modelId == null) return null;
        Path p = getBlockymodelPathFromAnyPack(modelId);
        if (p == null) return null;
        return load(p);
    }

    public boolean saveBlockymodel(BlockymodelBase base, Path p) {
        if (!p.isAbsolute()) {
            p = p.toAbsolutePath().normalize();
        }

        BsonUtil.writeDocument(p, BlockymodelBase.CODEC.encode(base, new ExtraInfo())).join();
        return true;
    }

    public Path getBlockymodelPathForPack(@Nonnull String name) {
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (!pack.getName().equals(name)) continue;
            Path blockmodelPackPath = this.getBlockymodelPathForPack(pack);
            if (Files.isDirectory(blockmodelPackPath)) return blockmodelPackPath;
        }

        return null;
    }

    private Path getBlockymodelPathForPack(@Nonnull AssetPack pack) {
        return pack.getRoot().resolve("Common");
    }

    private BlockymodelBase load(Path path) {
        Path p = path.toAbsolutePath().normalize();
        if (!Files.exists(p)) return null;

        BsonDocument doc = BsonUtil.readDocument(p).join();
        return BlockymodelBase.CODEC.decode(doc, new ExtraInfo());
    }

    private Path getHytaleAssetRootPath() {
        return AssetModule.get().getBaseAssetPack().getRoot();
    }

    private Path getBlockymodelPathFromAnyPack(@Nonnull String name) {
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path blockmodelPackPath = this.getBlockymodelPathForPack(pack);
            Path blockmodelPath = blockmodelPackPath.resolve(name);
            if (Files.exists(blockmodelPath)) return blockmodelPath;
        }

        return null;
    }

    private String getItemModelId(@Nonnull Item item) {
        String modelId = item.getModel();
        if (modelId == null && item.hasBlockType()) {
            return getItemModelId(item.getId());
        }
        return modelId;
    }

    private String getItemModelId(String blockTypeKey) {
        BlockType blockType = BlockType.getAssetMap().getAsset(blockTypeKey);
        if (blockType != null && blockType.getCustomModel() != null) {
            return blockType.getCustomModel();
        }
        return null;
    }

    private Item getItemFromAssetsById(String id) {
        return Item.getAssetMap().getAsset(id);
    }

    public void scaleBlockymodel(BlockymodelBase blockymodel, double scale) {
        Blockymodel[] nodes = blockymodel.getNodes();

        for (Blockymodel node : nodes) {
            scaleBlockymodel(node, scale);
        }
    }


    public void posBlockymodel(BlockymodelBase blockymodel, BlockymodelVector3d pos) {
        Blockymodel[] nodes = blockymodel.getNodes();

        for (Blockymodel node : nodes) {
            posBlockymodel(node, pos);
        }
    }

    private void scaleBlockymodel(Blockymodel model, double scale) {
        if (model.position != null) {
            model.position.scale(scale);
        }

        if (model.shape != null) {
            if (model.shape.settings != null) {
                if (model.shape.settings.size != null) {
                    model.shape.settings.size.scale(scale);
                }
            }
        }

        if (model.children != null) {
            for (Blockymodel child : model.children) {
                scaleBlockymodel(child, scale);
            }
        }
    }

    private void posBlockymodel(Blockymodel model, BlockymodelVector3d pos) {
        if (model.position != null) {
            model.position.add(pos);
        }

        if (model.children != null) {
            for (Blockymodel child : model.children) {
                posBlockymodel(child, pos);
            }
        }
    }
}
