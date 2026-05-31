package dev.marggx.mcreator.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.ModelCreatorPlugin;

import javax.annotation.Nullable;
import java.util.UUID;

public class GroupMembershipComponent implements Component<EntityStore> {
    private UUID uuid;

    public static final BuilderCodec<GroupMembershipComponent> CODEC = BuilderCodec
            .builder(GroupMembershipComponent.class, GroupMembershipComponent::new)
            .append(
                    new KeyedCodec<>("UUID", Codec.UUID_BINARY),
                    (component, value) -> component.uuid = value,
                    component -> component.uuid
            ).add()
            .build();

    public static ComponentType<EntityStore, GroupMembershipComponent> getComponentType() {
        return ModelCreatorPlugin.get().getGroupMembershipComponentType();
    }

    public GroupMembershipComponent() {
        this.uuid = UUID.randomUUID();
    }

    public GroupMembershipComponent(UUID uuid) {
        this.uuid = uuid;
    }

    public GroupMembershipComponent(GroupMembershipComponent other) {
        this.uuid = other.uuid;
    }

    public void setGroupUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getGroupUuid() {
        return uuid;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new GroupMembershipComponent(this);
    }
}
