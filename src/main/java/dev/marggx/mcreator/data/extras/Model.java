package dev.marggx.mcreator.data.extras;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.data.blockymodel.BlockymodelBase;

public class Model {
    private String id;
    private BlockymodelBase blockymodel;
    private Holder<EntityStore> holder;
    private String path;
    private String texturePath;
    private ModelType type;

    public Model() {
    }

    public Model(String id, BlockymodelBase blockymodel, Holder<EntityStore> holder, String path, String texturePath, ModelType type) {
        this.id = id;
        this.blockymodel = blockymodel;
        this.holder = holder;
        this.path = path;
        this.texturePath = texturePath;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public BlockymodelBase blockymodel() {
        return blockymodel;
    }

    public Holder<EntityStore> holder() {
        return holder;
    }

    public String path() {
        return path;
    }

    public String texturePath() {
        return texturePath;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBlockymodel(BlockymodelBase blockymodel) {
        this.blockymodel = blockymodel;
    }

    public void setHolder(Holder<EntityStore> holder) {
        this.holder = holder;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public boolean validate() {
        return id != null && holder != null && path != null && texturePath != null;
    }

    public ModelType getType() {
        return type;
    }

    public void setType(ModelType type) {
        this.type = type;
    }

    public enum ModelType {
        ENTITY,
        MODEL
    }
}