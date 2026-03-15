package dev.marggx.mcreator.data.extras;

import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import dev.marggx.mcreator.data.blockymodel.Blockymodel;
import dev.marggx.mcreator.services.TextureService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public class BaseModel {

    private int blockyId;
    private BlockSelection selection;
    private String name;
    private String pack;
    private List<Blockymodel> blockymodels = new ObjectArrayList<>();
    private BufferedImage texture;
    private Map<String, Integer> textureCache;

    public BaseModel() {
    }

    public BaseModel(int blockyId, BlockSelection selection, String name, String pack, List<Blockymodel> blockymodels, BufferedImage texture) {
        this.blockyId = blockyId;
        this.selection = selection;
        this.name = name;
        this.pack = pack;
        this.blockymodels = blockymodels;
        this.texture = texture;
    }

    public int blockyId() {
        return blockyId;
    }

    public String getStrBlockyId() {
        return String.valueOf(blockyId);
    }

    public void incrementBlockyId() {
        this.blockyId++;
    }

    public List<Blockymodel> blockymodels() {
        return blockymodels;
    }

    public BufferedImage texture() {
        return texture;
    }

    public BlockSelection selection() {
        return selection;
    }

    public String name() {
        return name;
    }

    public String pack() {
        return pack;
    }

    public void setBlockymodels(List<Blockymodel> blockymodels) {
        this.blockymodels = blockymodels;
    }

    public void addBlockymodel(Blockymodel blockymodel) {
        if (this.blockymodels == null) this.blockymodels = new ObjectArrayList<>();
        this.blockymodels.add(blockymodel);
    }

    public void addBlockymodels(List<Blockymodel> blockymodels) {
        if (this.blockymodels == null) this.blockymodels = new ObjectArrayList<>();
        this.blockymodels.addAll(blockymodels);
    }

    public void setTexture(BufferedImage texture) {
        this.texture = texture;
    }

    public void extendTexture(BufferedImage newTexture) {
        if (this.texture == null) {
            this.texture = newTexture;
            return;
        }
        this.texture = TextureService.get().combineImages(this.texture, newTexture);
    }

    public void setSelection(BlockSelection selection) {
        this.selection = selection;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public boolean validate() {
        return name != null && pack != null && !blockymodels.isEmpty() && texture != null;
    }

    public void addToTextureCache(String textureKey, int position) {
        if (this.textureCache == null) this.textureCache = new java.util.HashMap<>();
        this.textureCache.put(textureKey, position);
    }

    public int getFromTextureCache(String textureKey) {
        if (this.textureCache == null) return -1;
        Integer cached = this.textureCache.get(textureKey);
        return cached != null ? cached : -1;
    }
}
