package dev.marggx.mcreator.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

public class BlockymodelShapeTextureLayoutVector2b {
    @Nonnull
    public static final BuilderCodec<BlockymodelShapeTextureLayoutVector2b> CODEC = BuilderCodec.builder(BlockymodelShapeTextureLayoutVector2b.class, BlockymodelShapeTextureLayoutVector2b::new)
            .append(new KeyedCodec<>("x", Codec.BOOLEAN, true, true), (i, v) -> i.x = v, i -> i.x)
            .add()
            .append(new KeyedCodec<>("y", Codec.BOOLEAN, true, true), (i, v) -> i.y = v, i -> i.y)
            .add()
            .build();

    public boolean x;
    public boolean y;

    public BlockymodelShapeTextureLayoutVector2b() {}

    public BlockymodelShapeTextureLayoutVector2b(boolean x, boolean y) {
        this.x = x;
        this.y = y;
    }

    public boolean isX() {
        return this.x;
    }

    public boolean isY() {
        return this.y;
    }
}
