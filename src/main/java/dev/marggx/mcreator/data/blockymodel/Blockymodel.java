package dev.marggx.mcreator.data.blockymodel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import dev.marggx.mcreator.codec.OnDemandCodec;

import javax.annotation.Nonnull;

public class Blockymodel {
    @Nonnull
    public static final BuilderCodec<Blockymodel> CODEC = BuilderCodec.builder(Blockymodel.class, Blockymodel::new)
            .append(new KeyedCodec<>("id", Codec.STRING, true, true), (i, v) -> i.id = v, i -> i.id)
            .add()
            .append(new KeyedCodec<>("name", Codec.STRING, true, true), (i, v) -> i.name = v, i -> i.name)
            .add()
            .append(new KeyedCodec<>("position", BlockymodelVector3d.CODEC, true, true), (i, v) -> i.position = v, i -> i.position)
            .add()
            .append(new KeyedCodec<>("orientation", BlockymodelQuaternion.CODEC, true, true), (i,v) -> i.orientation = v, i -> i.orientation)
            .add()
            .append(new KeyedCodec<>("shape", BlockymodelShape.CODEC, false, true), (i,v) -> i.shape = v, i -> i.shape)
            .add()
            .append(new KeyedCodec<>("children", new ArrayCodec<>(new OnDemandCodec<>(() -> Blockymodel.CODEC), Blockymodel[]::new), false, true), (i, v) -> i.children = v, i -> i.children)
            .add()
            .build();

    public String id;
    public String name;
    public BlockymodelVector3d position;
    public BlockymodelQuaternion orientation;
    public BlockymodelShape shape;
    public Blockymodel[] children;

    public Blockymodel() {
    }

    public Blockymodel(String id, String name, BlockymodelVector3d position, BlockymodelQuaternion orientation, BlockymodelShape shape, Blockymodel[] children) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.orientation = orientation;
        this.shape = shape;
        this.children = children;
    }
}
