package dev.marggx.mcomb.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

public class BlockymodelShapeSettings {
    @Nonnull
    public static final BuilderCodec<BlockymodelShapeSettings> CODEC = BuilderCodec.builder(BlockymodelShapeSettings.class, BlockymodelShapeSettings::new)
            .append(new KeyedCodec<>("size", BlockymodelVector3d.CODEC, false, true), (i, v) -> i.size = v, i -> i.size)
            .add()
            .append(new KeyedCodec<>("normal", Codec.STRING, false, true), (i, v) -> i.normal = BlockymodelShapeSettingsNormal.fromValue(v), (i) -> {return i.normal == null ? null : i.normal.getValue();})
            .add()
            .append(new KeyedCodec<>("isPiece", Codec.BOOLEAN, false, true), (i, v) -> i.isPiece = v, i -> i.isPiece)
            .add()
            .append(new KeyedCodec<>("isStaticBox", Codec.BOOLEAN, false, true), (i, v) -> i.isStaticBox = v, i -> i.isStaticBox)
            .add()
            .build();

    public BlockymodelVector3d size;
    public BlockymodelShapeSettingsNormal normal;
    public boolean isPiece;
    public boolean isStaticBox;

    public BlockymodelShapeSettings() {}

    public BlockymodelShapeSettings(BlockymodelVector3d size, BlockymodelShapeSettingsNormal normal, boolean isPiece, boolean isStaticBox) {
        this.size = size;
        this.normal = normal;
        this.isPiece = isPiece;
        this.isStaticBox = isStaticBox;
    }
}
