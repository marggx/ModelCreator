package dev.marggx.mcreator.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class BlockymodelShape {
    @Nonnull
    public static final BuilderCodec<BlockymodelShape> CODEC = BuilderCodec.builder(BlockymodelShape.class, BlockymodelShape::new)
            .append(new KeyedCodec<>("offset", BlockymodelVector3d.CODEC, true, true), (i, v) -> i.offset = v, i -> i.offset)
            .add()
            .append(new KeyedCodec<>("stretch", BlockymodelVector3d.CODEC, true, true), (i, v) -> i.stretch = v, i -> i.stretch)
            .add()
            .append(
                    new KeyedCodec<>("textureLayout", new MapCodec<>(BlockymodelShapeTextureLayout.CODEC, HashMap::new), true, true),
                    (i, v) -> i.textureLayout = v,
                    i -> i.textureLayout
            )
            .add()
            .append(new KeyedCodec<>("type", Codec.STRING, true, true), (i, v) -> i.type = BlockymodelShapeType.fromValue(v), (i) -> {return i.type == null ? null : i.type.getValue();})
            .add()
            .append(new KeyedCodec<>("settings", BlockymodelShapeSettings.CODEC, true, true), (i, v) -> i.settings = v, i -> i.settings)
            .add()
            .append(new KeyedCodec<>("unwrapMode", Codec.STRING, true, true), (i, v) -> i.unwrapMode = "custom", i -> i.unwrapMode)
            .add()
            .append(new KeyedCodec<>("visible", Codec.BOOLEAN, true, true), (i, v) -> i.visible = true, i -> i.visible)
            .add()
            .append(new KeyedCodec<>("doubleSided", Codec.BOOLEAN, true, true), (i, v) -> i.doubleSided = false, i -> i.doubleSided)
            .add()
            .append(new KeyedCodec<>("shadingMode", Codec.STRING, true, true), (i, v) -> i.shadingMode = BlockymodelShapeShadingMode.fromValue(v), (i) -> {return i.shadingMode == null ? null : i.shadingMode.getValue();})
            .add()
            .build();

    public BlockymodelVector3d offset;
    public BlockymodelVector3d stretch;
    public Map<String, BlockymodelShapeTextureLayout> textureLayout;
    public BlockymodelShapeType type;
    public BlockymodelShapeSettings settings;
    public String unwrapMode = "custom";
    public boolean visible = true;
    public boolean doubleSided = false;
    public BlockymodelShapeShadingMode shadingMode = BlockymodelShapeShadingMode.Flat;

    public BlockymodelShape() {
    }

    public BlockymodelShape(BlockymodelVector3d offset, BlockymodelVector3d stretch, Map<String, BlockymodelShapeTextureLayout> textureLayout, BlockymodelShapeType type, BlockymodelShapeSettings settings) {
        this.offset = offset;
        this.stretch = stretch;
        this.textureLayout = textureLayout;
        this.type = type;
        this.settings = settings;
    }
}
