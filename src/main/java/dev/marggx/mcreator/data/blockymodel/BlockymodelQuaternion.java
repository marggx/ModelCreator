package dev.marggx.mcreator.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.util.TrigMathUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import javax.annotation.Nonnull;

public class BlockymodelQuaternion {
    @Nonnull
    public static final BuilderCodec<BlockymodelQuaternion> CODEC = BuilderCodec.builder(BlockymodelQuaternion.class, BlockymodelQuaternion::new)
            .append(new KeyedCodec<>("x", Codec.DOUBLE, true, true), (i, v) -> i.x = v, i -> i.x)
            .add()
            .append(new KeyedCodec<>("y", Codec.DOUBLE, true, true), (i, v) -> i.y = v, i -> i.y)
            .add()
            .append(new KeyedCodec<>("z", Codec.DOUBLE, true, true), (i, v) -> i.z = v, i -> i.z)
            .add()
            .append(new KeyedCodec<>("w", Codec.DOUBLE, true, true), (i, v) -> i.w = v, i -> i.w)
            .add()
            .build();

    public double x;
    public double y;
    public double z;
    public double w;

    public BlockymodelQuaternion() {
    }

    public BlockymodelQuaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public BlockymodelQuaternion(Quaterniondc quad) {
        this.x = quad.x();
        this.y = quad.y();
        this.z = quad.z();
        this.w = quad.w();
    }

    public static Quaterniond fromVector3d(Vector3d rot) {
        return new Quaterniond().rotationYXZ(rot.y(), rot.x(), rot.z());
    }


    public static BlockymodelQuaternion getLocalQuat(Quaterniond baseQuad, Rotation3f toLocalRotation) {
        Vector3d toLocal = new Vector3d(toLocalRotation.x(), toLocalRotation.y(), toLocalRotation.z());
        toLocal.rotateY(TrigMathUtil.PI);

        Quaterniond originalQuat = new Quaterniond(baseQuad);
        originalQuat.invert();
        Quaterniond toLocalQuat = BlockymodelQuaternion.fromVector3d(toLocal);
        originalQuat.mul(toLocalQuat);
        return new BlockymodelQuaternion(originalQuat);
    }
}
