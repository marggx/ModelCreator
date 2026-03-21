package dev.marggx.mcreator.data.blockymodel;


import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;

public class BlockymodelBase {
    @Nonnull
    public static final BuilderCodec<BlockymodelBase> CODEC = BuilderCodec.builder(BlockymodelBase.class, BlockymodelBase::new)
            .append(new KeyedCodec<>("format", Codec.STRING, false, true), (i, v) -> i.format = v, i -> i.format)
            .add()
            .append(new KeyedCodec<>("lod", Codec.STRING, false, true), (i, v) -> i.lod = "auto", i -> i.lod)
            .add()
            .append(new KeyedCodec<>("nodes", new ArrayCodec<>(Blockymodel.CODEC, Blockymodel[]::new), true, true), (i, v) -> i.nodes = v, i -> i.nodes)
            .add()
            .build();

    public String format;
    public String lod = "auto";
    public Blockymodel[] nodes;

    public BlockymodelBase() {
    }

    public BlockymodelBase(String format, Blockymodel[] nodes) {
        this.format = format;
        this.nodes = nodes;
    }

    public String getFormat() {
        return format;
    }

    public Blockymodel[] getNodes() {
        return nodes;
    }

    public void addNode(Blockymodel node) {
        nodes = java.util.Arrays.copyOf(nodes, nodes.length + 1);
        nodes[nodes.length - 1] = node;
    }

    public String getLod() {
        return lod;
    }
}
