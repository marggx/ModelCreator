package dev.marggx.mcreator.services;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPacketHandler;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolSetEntityScale;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolSetEntityTransform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.io.handlers.IPacketHandler;
import com.hypixel.hytale.server.core.io.handlers.IWorldPacketHandler;
import com.hypixel.hytale.server.core.io.handlers.SubPacketHandler;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.components.GroupMembershipComponent;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class PacketHandler implements SubPacketHandler {

    private static final Message MESSAGE_BUILDER_TOOLS_USAGE_DENIED = Message.translation("server.builderTools.usageDenied");
    private final IPacketHandler packetHandler;
    private final BuilderToolsPacketHandler builderToolsPacketHandler;

    public PacketHandler(@Nonnull IPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.builderToolsPacketHandler = new BuilderToolsPacketHandler(packetHandler);
    }

    private static boolean hasPermission(@Nonnull PlayerRef playerRef) {
        return hasPermission(playerRef, null);
    }

    private static boolean hasPermission(@Nonnull PlayerRef playerRef, @Nullable String additionalPermission) {
        UUID playerUuid = playerRef.getUuid();
        PermissionsModule permissionsModule = PermissionsModule.get();
        boolean hasBuilderToolsEditor = permissionsModule.hasPermission(playerUuid, HytalePermissions.BUILDER_TOOLS_EDITOR);
        boolean hasAdditionalPerm = additionalPermission != null && permissionsModule.hasPermission(playerUuid, additionalPermission);
        if (!hasBuilderToolsEditor && !hasAdditionalPerm) {
            playerRef.sendMessage(MESSAGE_BUILDER_TOOLS_USAGE_DENIED);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void registerHandlers() {
        if (BuilderToolsPlugin.get().isDisabled()) {
            return;
        }

        IWorldPacketHandler.registerHandler(this.packetHandler, 402, this::handleBuilderToolSetEntityTransform, PacketHandler::hasPermission);
        IWorldPacketHandler.registerHandler(this.packetHandler, 420, this::handleBuilderToolSetEntityScale, PacketHandler::hasPermission);
    }

    public void handleBuilderToolSetEntityTransform(
            @Nonnull BuilderToolSetEntityTransform packet,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull World world,
            @Nonnull Store<EntityStore> store
    ) {
        if (packet.modelTransform == null || packet.modelTransform.position == null) {
            builderToolsPacketHandler.handleBuilderToolSetEntityTransform(packet, playerRef, ref, world, store);
            return;
        }

        Ref<EntityStore> targetRef = store.getExternalData().getRefFromNetworkId(packet.entityId);
        assert targetRef != null;

        GroupMembershipComponent groupMembershipComponent = store.getComponent(targetRef, GroupMembershipComponent.getComponentType());
        if (groupMembershipComponent == null) {
            builderToolsPacketHandler.handleBuilderToolSetEntityTransform(packet, playerRef, ref, world, store);
            return;
        }

        Ref<EntityStore> groupRef = world.getEntityStore().getRefFromUUID(groupMembershipComponent.getGroupUuid());
        if (groupRef == null) {
            builderToolsPacketHandler.handleBuilderToolSetEntityTransform(packet, playerRef, ref, world, store);
            return;
        }

        assert packet.modelTransform.position != null;
        Vector3d newPos = new Vector3d(packet.modelTransform.position.x, packet.modelTransform.position.y, packet.modelTransform.position.z);
        GroupService.get().moveGroup(ref, targetRef, groupRef, newPos, store);

        builderToolsPacketHandler.handleBuilderToolSetEntityTransform(packet, playerRef, ref, world, store);
    }

    public void handleBuilderToolSetEntityScale(
            @Nonnull BuilderToolSetEntityScale packet,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull World world,
            @Nonnull Store<EntityStore> store
    ) {
        Ref<EntityStore> targetRef = store.getExternalData().getRefFromNetworkId(packet.entityId);
        assert targetRef != null;

        GroupMembershipComponent groupMembershipComponent = store.getComponent(targetRef, GroupMembershipComponent.getComponentType());
        if (groupMembershipComponent == null) {
            builderToolsPacketHandler.handleBuilderToolSetEntityScale(packet, playerRef, ref, world, store);
            return;
        }

        Ref<EntityStore> groupRef = world.getEntityStore().getRefFromUUID(groupMembershipComponent.getGroupUuid());
        if (groupRef == null) {
            builderToolsPacketHandler.handleBuilderToolSetEntityScale(packet, playerRef, ref, world, store);
            return;
        }

        GroupService.get().scaleGroup(ref, targetRef, groupRef, packet.scale, store);
        builderToolsPacketHandler.handleBuilderToolSetEntityScale(packet, playerRef, ref, world, store);
    }
}
