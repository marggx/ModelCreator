package dev.marggx.mcreator.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

public class BlockymodelShapeTextureLayout {
    @Nonnull
    public static final BuilderCodec<BlockymodelShapeTextureLayout> CODEC = BuilderCodec.builder(BlockymodelShapeTextureLayout.class, BlockymodelShapeTextureLayout::new)
            .append(new KeyedCodec<>("offset", BlockymodelVector2i.CODEC, true, true), (i, v) -> i.offset = v, i -> i.offset)
            .add()
            .append(new KeyedCodec<>("mirror", BlockymodelShapeTextureLayoutVector2b.CODEC, true, true), (i, v) -> i.mirror = v, i -> i.mirror)
            .add()
            .append(new KeyedCodec<>("angle", Codec.INTEGER, true, true), (i, v) -> i.angle = v, i -> i.angle)
            .add()
            .build();

    public BlockymodelVector2i offset;
    public BlockymodelShapeTextureLayoutVector2b mirror;
    public int angle;

    public BlockymodelShapeTextureLayout() {}

    public BlockymodelShapeTextureLayout(BlockymodelVector2i offset, BlockymodelShapeTextureLayoutVector2b mirror, int angle) {
        this.offset = offset;
        this.mirror = mirror;
        this.angle = angle;
    }
}
