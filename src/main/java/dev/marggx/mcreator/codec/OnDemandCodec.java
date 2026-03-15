package dev.marggx.mcreator.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.WrappedCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import org.bson.BsonValue;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.function.Supplier;

public class OnDemandCodec<T> implements Codec<T>, WrappedCodec<T> {
    private final Supplier<Codec<T>> getter;

    public OnDemandCodec(Supplier<Codec<T>> getter) {
         this.getter = getter;
    }

    @NullableDecl
    @Override
    public T decode(BsonValue bsonValue, ExtraInfo extraInfo) {
        Codec<T> codec = getter.get();
        return codec.decode(bsonValue, extraInfo);
    }

    @Override
    public BsonValue encode(T t, ExtraInfo extraInfo) {
        Codec<T> codec = getter.get();
        return codec.encode(t, extraInfo);
    }

    @Override
    public Codec<T> getChildCodec() {
        return getter.get();
    }

    @NonNullDecl
    @Override
    public Schema toSchema(@NonNullDecl SchemaContext schemaContext) {
        return new Schema();
    }
}
