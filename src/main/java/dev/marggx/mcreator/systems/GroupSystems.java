package dev.marggx.mcreator.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.components.GroupComponent;
import dev.marggx.mcreator.components.GroupMembershipComponent;
import dev.marggx.mcreator.services.GroupService;

import javax.annotation.Nonnull;

public class GroupSystems {

    public GroupSystems() {
    }

    public static class GroupEntityRemoved extends RefSystem<EntityStore> {
        private final ComponentType<EntityStore, UUIDComponent> uuidComponentType = UUIDComponent.getComponentType();
        private final ComponentType<EntityStore, GroupComponent> groupComponentType = GroupComponent.getComponentType();
        private final Archetype<EntityStore> archetype;

        public GroupEntityRemoved() {
            this.archetype = Archetype.of(this.uuidComponentType, this.groupComponentType);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return this.archetype;
        }

        @Override
        public void onEntityAdded(
                @Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
        }

        @Override
        public void onEntityRemove(
                @Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            GroupComponent groupComponent = commandBuffer.getComponent(ref, this.groupComponentType);

            assert groupComponent != null;
            if (groupComponent.size() == 0) return;

            groupComponent.forEachMember((refMember) -> {
                commandBuffer.tryRemoveComponent(refMember, GroupMembershipComponent.getComponentType());
            }, null);
        }
    }

    public static class GroupMemberEntityRef extends RefSystem<EntityStore> {
        @Nonnull
        private final ComponentType<EntityStore, GroupMembershipComponent> groupMembershipComponentComponentType;

        public GroupMemberEntityRef(@Nonnull ComponentType<EntityStore, GroupMembershipComponent> groupMembershipComponentComponentType) {
            this.groupMembershipComponentComponentType = groupMembershipComponentComponentType;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return this.groupMembershipComponentComponentType;
        }

        @Override
        public void onEntityAdded(
                @Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            GroupMembershipComponent groupMembershipComponent = commandBuffer.getComponent(ref, this.groupMembershipComponentComponentType);
            assert groupMembershipComponent != null;
            commandBuffer.run(_store -> {
                GroupService.get().joinGroup(ref, _store, groupMembershipComponent.getGroupUuid(), null);
            });
        }

        @Override
        public void onEntityRemove(
                @Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            GroupMembershipComponent groupMembershipComponent = commandBuffer.getComponent(ref, this.groupMembershipComponentComponentType);
            assert groupMembershipComponent != null;
            Ref<EntityStore> groupRef = commandBuffer.getExternalData().getRefFromUUID(groupMembershipComponent.getGroupUuid());
            commandBuffer.run(_store -> {
                GroupService.get().removeEntityFromGroup(ref, _store, groupRef);
            });
        }

    }
}
