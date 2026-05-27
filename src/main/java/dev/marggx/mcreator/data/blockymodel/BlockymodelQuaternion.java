package dev.marggx.mcreator.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Quaterniond;
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

    public static BlockymodelQuaternion fromRotation3f(Rotation3f rot) {
        Vector3d rotV3 = new Vector3d(rot.x(), rot.y(), rot.z());
        rotV3.rotateY((float)Math.PI);

        Quaterniond quat = new Quaterniond().rotationYXZ(rotV3.y(), rotV3.x(), rotV3.z());
        return new BlockymodelQuaternion(quat.x(), quat.y(), quat.z(), quat.w());
    }


    public static BlockymodelQuaternion getLocalQuat(Rotation3f baseRotation, Rotation3f toLocalRotation) {
        Vector3d baseOrientation = new Vector3d(baseRotation.x(), baseRotation.y(), baseRotation.z());
        baseOrientation.rotateY((float)Math.PI);

        Vector3d toLocal = new Vector3d(toLocalRotation.x(), toLocalRotation.y(), toLocalRotation.z());
        toLocal.rotateY((float)Math.PI);

        Quaterniond originalQuat = new Quaterniond().rotationYXZ(baseOrientation.y(), baseOrientation.x(), baseOrientation.z());
        originalQuat.invert();
        Quaterniond toLocalQuat = new Quaterniond().rotationYXZ(toLocal.y(), toLocal.x(), toLocal.z());
        Quaterniond result = originalQuat.mul(toLocalQuat);
        return new BlockymodelQuaternion(result.x(), result.y(), result.z(), result.w());
    }
}
