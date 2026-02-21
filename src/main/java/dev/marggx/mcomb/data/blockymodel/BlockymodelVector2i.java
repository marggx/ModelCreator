package dev.marggx.mcomb.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDisplayMode;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.vector.Vector2i;

import javax.annotation.Nonnull;

public class BlockymodelVector2i extends Vector2i {
    @Nonnull
    public static final BuilderCodec<BlockymodelVector2i> CODEC = BuilderCodec.builder(BlockymodelVector2i.class, BlockymodelVector2i::new)
            .metadata(UIDisplayMode.COMPACT)
            .appendInherited(new KeyedCodec<>("x", Codec.INTEGER, true, true), (o, i) -> o.x = i, o -> o.x, (o, p) -> o.x = p.x)
            .addValidator(Validators.nonNull())
            .add()
            .appendInherited(new KeyedCodec<>("y", Codec.INTEGER, true, true), (o, i) -> o.y = i, o -> o.y, (o, p) -> o.y = p.y)
            .addValidator(Validators.nonNull())
            .add()
            .build();
}
