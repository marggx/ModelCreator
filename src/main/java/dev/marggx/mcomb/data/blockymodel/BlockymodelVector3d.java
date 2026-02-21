package dev.marggx.mcomb.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDisplayMode;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.vector.Vector3d;

import javax.annotation.Nonnull;

public class BlockymodelVector3d extends Vector3d {
    @Nonnull
    public static final BuilderCodec<BlockymodelVector3d> CODEC = BuilderCodec.builder(BlockymodelVector3d.class, BlockymodelVector3d::new)
            .metadata(UIDisplayMode.COMPACT)
            .<Double>appendInherited(new KeyedCodec<>("x", Codec.DOUBLE, true, true), (o, i) -> o.x = i, o -> o.x, (o, p) -> o.x = p.x)
            .addValidator(Validators.nonNull())
            .add()
            .<Double>appendInherited(new KeyedCodec<>("y", Codec.DOUBLE, true, true), (o, i) -> o.y = i, o -> o.y, (o, p) -> o.y = p.y)
            .addValidator(Validators.nonNull())
            .add()
            .<Double>appendInherited(new KeyedCodec<>("z", Codec.DOUBLE, true, true), (o, i) -> o.z = i, o -> o.z, (o, p) -> o.z = p.z)
            .addValidator(Validators.nonNull())
            .add()
            .build();

    public BlockymodelVector3d() {
        super(0.0, 0.0, 0.0);
    }

    public BlockymodelVector3d(float q) {
        super(q, q, q);
    }

    public BlockymodelVector3d(double x, double y, double z) {
        super(x, y, z);
    }

    public static BlockymodelVector3d from(Vector3d v3d) {
        return new BlockymodelVector3d(v3d.getX(), v3d.getY(), v3d.getZ());
    }
}
