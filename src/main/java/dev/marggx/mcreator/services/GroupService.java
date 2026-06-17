package dev.marggx.mcreator.services;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.components.GroupComponent;
import dev.marggx.mcreator.components.GroupMembershipComponent;
import dev.marggx.mcreator.data.blockymodel.BlockymodelBase;
import dev.marggx.mcreator.data.extras.Model;
import dev.marggx.mcreator.utils.Logger;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.*;

public class GroupService {

    private static final double MAX_DIST_FOR_AUTO_GROUPS = 1.5;
    private static final GroupService INSTANCE = new GroupService();
    private static final Logger LOGGER = Logger.get();

    public static GroupService get() {
        return INSTANCE;
    }

    private final BlockymodelService blockymodelService = BlockymodelService.get();

    public GroupService() {
    }

    public void createAndJoinGroup(@Nonnull List<Ref<EntityStore>> entities, @Nonnull Store<EntityStore> store, UUID uuid) {
        Ref<EntityStore> groupRef = this.createGroup(store, uuid);
        UUIDComponent uuidComponent = store.getComponent(groupRef, UUIDComponent.getComponentType());
        assert uuidComponent != null;
        UUID groupUuid = uuidComponent.getUuid();

        for (Ref<EntityStore> entityRef : entities) {
            this.joinGroup(entityRef, store, groupUuid, groupRef);
        }
    }

    public Ref<EntityStore> createGroup(@Nonnull Store<EntityStore> store, UUID uuid) {
        if (uuid == null) uuid = UUID.randomUUID();
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
        holder.addComponent(GroupComponent.getComponentType(), new GroupComponent());
        return store.addEntity(holder, AddReason.LOAD);
    }

    public void deleteGroup(@Nonnull UUID uuid, @Nonnull Store<EntityStore> store) {
        Ref<EntityStore> groupRef = store.getExternalData().getRefFromUUID(uuid);
        if (groupRef == null) {
            return;
        }
        this.deleteGroup(groupRef, store);
    }

    public void deleteGroup(@Nonnull Ref<EntityStore> groupRef, @Nonnull Store<EntityStore> store) {
        GroupComponent groupComponent = store.getComponent(groupRef, GroupComponent.getComponentType());
        assert groupComponent != null;

        for (Ref<EntityStore> entityRef : groupComponent.getMemberList()) {
            store.tryRemoveComponent(entityRef, GroupMembershipComponent.getComponentType());
        }

        store.removeEntity(groupRef, EntityStore.REGISTRY.newHolder(), RemoveReason.REMOVE);
    }

    public void joinGroup(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store, @Nonnull UUID uuid, Ref<EntityStore> groupRef) {
        GroupMembershipComponent groupMembershipComponent = store.getComponent(entityRef, GroupMembershipComponent.getComponentType());

        if (groupMembershipComponent != null && !groupMembershipComponent.getGroupUuid().equals(uuid)) {
            this.leaveGroup(entityRef, store, null);
        }

        store.putComponent(entityRef, GroupMembershipComponent.getComponentType(), new GroupMembershipComponent(uuid));

        if (groupRef == null) {
            groupRef = store.getExternalData().getRefFromUUID(uuid);
        }

        if (groupRef == null) {
            groupRef = this.createGroup(store, uuid);
        }

        GroupComponent groupComponent = store.getComponent(groupRef, GroupComponent.getComponentType());
        assert groupComponent != null;

        if (groupComponent.contains(entityRef)) {
            return;
        }

        groupComponent.add(entityRef);
    }

    public void leaveGroup(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store, Ref<EntityStore> groupRef) {
        GroupMembershipComponent groupMembershipComponent = store.getComponent(entityRef, GroupMembershipComponent.getComponentType());

        if (groupRef == null && groupMembershipComponent == null) {
            return;
        } else if (groupRef == null) {
            groupRef = store.getExternalData().getRefFromUUID(groupMembershipComponent.getGroupUuid());
        }

        if (groupMembershipComponent == null) {
            this.removeEntityFromGroup(entityRef, store, groupRef);
            return;
        }

        store.tryRemoveComponent(entityRef, GroupMembershipComponent.getComponentType());
        this.removeEntityFromGroup(entityRef, store, groupRef);
    }

    public void removeEntityFromGroup(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store, Ref<EntityStore> groupRef) {
        if (groupRef == null || !groupRef.isValid()) {
            return;
        }
        GroupComponent groupComponent = store.getComponent(groupRef, GroupComponent.getComponentType());
        assert groupComponent != null;
        groupComponent.remove(entityRef);
        if (groupComponent.size() == 0) {
            store.removeEntity(groupRef, EntityStore.REGISTRY.newHolder(), RemoveReason.REMOVE);
        }
    }

    public void moveGroup(
            @Nonnull Ref<EntityStore> playerRef,
            Ref<EntityStore> targetRef,
            Ref<EntityStore> groupRef,
            Vector3d newPos,
            @Nonnull Store<EntityStore> store
    ) {
        Player playerComponent = store.getComponent(playerRef, Player.getComponentType());
        assert playerComponent != null;
        PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
        assert playerRefComponent != null;

        BuilderToolsPlugin.addToQueue(
                playerComponent,
                playerRefComponent,
                (r, builderState, componentAccessor) -> {
                    TransformComponent transformComponent = componentAccessor.getComponent(targetRef, TransformComponent.getComponentType());
                    if (transformComponent == null) {
                        return;
                    }

                    Vector3d pos = new Vector3d(transformComponent.getPosition());
                    Vector3d diff = pos.sub(newPos.x, newPos.y, newPos.z);

                    GroupComponent groupComponent = componentAccessor.getComponent(groupRef, GroupComponent.getComponentType());
                    assert groupComponent != null;

                    groupComponent.forEachMember((memberRef) -> {
                        TransformComponent memberTransformComponent = componentAccessor.getComponent(memberRef, TransformComponent.getComponentType());
                        assert memberTransformComponent != null;

                        memberTransformComponent.getPosition().sub(diff);
                        memberTransformComponent.markChunkDirty(componentAccessor);
                    }, targetRef);
                });
    }


    public void scaleGroup(
            @Nonnull Ref<EntityStore> playerRef,
            Ref<EntityStore> targetRef,
            Ref<EntityStore> groupRef,
            float newScale,
            @Nonnull Store<EntityStore> store
    ) {
        Player playerComponent = store.getComponent(playerRef, Player.getComponentType());
        assert playerComponent != null;
        PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
        assert playerRefComponent != null;

        BuilderToolsPlugin.addToQueue(
                playerComponent,
                playerRefComponent,
                (r, builderState, componentAccessor) -> {
                    if (!targetRef.isValid()) {
                        return;
                    }

                    EntityScaleComponent scaleComponent = componentAccessor.getComponent(targetRef, EntityScaleComponent.getComponentType());
                    if (scaleComponent == null) {
                        return;
                    }

                    float diff = newScale - scaleComponent.getScale();

                    GroupComponent groupComponent = componentAccessor.getComponent(groupRef, GroupComponent.getComponentType());
                    assert groupComponent != null;

                    groupComponent.forEachMember((memberRef) -> {
                        EntityScaleComponent memberScaleComponent = componentAccessor.getComponent(memberRef, EntityScaleComponent.getComponentType());
                        if (memberScaleComponent == null) {
                            memberScaleComponent = new EntityScaleComponent(newScale);
                            componentAccessor.addComponent(memberRef, EntityScaleComponent.getComponentType(), memberScaleComponent);
                            return;
                        }
                        memberScaleComponent.setScale(memberScaleComponent.getScale() + diff);
                    }, targetRef);
                });
    }

    public void autoCreateGroupsBySelection(@Nonnull BlockSelection selection, @Nonnull Store<EntityStore> store) {
        List<RefGroup> groups = createGroupsByPosAndNodeCount(selection, store, null);
        for (RefGroup group : groups) {
            this.createAndJoinGroup(group.entities, store, null);
        }
    }

    public List<RefGroup> createGroupsByPosAndNodeCount(@Nonnull BlockSelection selection, @Nonnull Store<EntityStore> store, Double maxDist) {
        List<RefGroup> groups = new ObjectArrayList<>();
        List<Ref<EntityStore>> refs = HytaleService.get().getRefsFromBlockSelection(selection, store);
        Set<Ref<EntityStore>> used = new HashSet<>();

        refs.sort(Comparator.comparingDouble(ref -> {
            Vector3d pos = getEntityPos(ref, store);
            return pos.y();
        }));

        for (Ref<EntityStore> starter : refs) {
            if (used.contains(starter)) {
                continue;
            }

            RefGroup group = new RefGroup(starter, store, maxDist != null ? maxDist : MAX_DIST_FOR_AUTO_GROUPS);
            used.add(starter);

            boolean changed;

            do {
                changed = false;

                for (Ref<EntityStore> ref : refs) {
                    if (used.contains(ref)) {
                        continue;
                    }

                    if (group.tryAddMember(ref, store)) {
                        used.add(ref);
                        changed = true;
                    }
                }
            }
            while (changed);

            groups.add(group);
        }
        return groups;
    }

    public List<ModelGroup> createGroupsByPosAndNodeCount(@Nonnull List<Model> models, Double maxDist) {
        List<ModelGroup> groups = new ObjectArrayList<>();
        Set<Model> used = new HashSet<>();

        models.sort(Comparator.comparingDouble(model -> {
            Vector3d pos = getEntityPos(model.holder());
            return pos.y();
        }));

        for (Model starter : models) {
            if (used.contains(starter)) {
                continue;
            }

            ModelGroup group = new ModelGroup(starter, maxDist != null ? maxDist : MAX_DIST_FOR_AUTO_GROUPS);
            used.add(starter);

            boolean changed;

            do {
                changed = false;

                for (Model model : models) {
                    if (used.contains(model)) {
                        continue;
                    }

                    if (group.tryAddMember(model)) {
                        used.add(model);
                        changed = true;
                    }
                }
            }
            while (changed);

            groups.add(group);
        }
        return groups;
    }

    private Vector3d getEntityPos(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        TransformComponent transformComponent = store.getComponent(entityRef, TransformComponent.getComponentType());
        assert transformComponent != null;

        return new Vector3d(transformComponent.getPosition());
    }

    private Vector3d getEntityPos(Holder<EntityStore> holder) {
        TransformComponent transformComponent = holder.getComponent(TransformComponent.getComponentType());
        assert transformComponent != null;

        return new Vector3d(transformComponent.getPosition());
    }

    public class RefGroup {
        public List<Ref<EntityStore>> entities = new ObjectArrayList<>();
        public final Vector3d center = new Vector3d();
        private final Vector3d tmp = new Vector3d();
        private final double maxDist;
        public int blockyNodeCount = 0;

        public RefGroup(Ref<EntityStore> firstRef, Store<EntityStore> store, double maxDist) {
            center.set(getEntityPos(firstRef, store));
            entities.add(firstRef);
            blockyNodeCount += getBlockyNodeCount(firstRef, store);
            this.maxDist = maxDist;
        }

        public boolean tryAddMember(@Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
            Vector3d pos = getEntityPos(entityRef, store);
            boolean inRange = center.distance(pos) <= maxDist;

            if (!inRange) {
                return false;
            }

            int memberNodeCount = getBlockyNodeCount(entityRef, store);

            boolean nodesExceedLimit = (memberNodeCount + blockyNodeCount) > (HytaleService.NODE_LIMIT - entities.size() - 1);

            if (nodesExceedLimit) {
                return false;
            }

            blockyNodeCount += memberNodeCount;

            entities.add(entityRef);

            pos.sub(center, tmp);
            center.fma(1.0 / entities.size(), tmp);
            return true;
        }

        private int getBlockyNodeCount(Ref<EntityStore> ref, Store<EntityStore> store) {
            Model model = blockymodelService.loadModelFromHolder(store.copyEntity(ref));
            BlockymodelBase blockymodelBase = blockymodelService.loadBlockymodelBase(model.path());
            int count = blockymodelService.countNodes(blockymodelBase);
            count += blockymodelService.countAttachmentsNodes(model);
            return count;
        }
    }

    public class ModelGroup {
        public List<Model> entities = new ObjectArrayList<>();
        public final Vector3d center = new Vector3d();
        private final Vector3d tmp = new Vector3d();
        private final double maxDist;
        public int blockyNodeCount = 0;

        public ModelGroup(Model firstModel, double maxDist) {
            center.set(getEntityPos(firstModel.holder()));
            entities.add(firstModel);
            blockyNodeCount += getBlockyNodeCount(firstModel.holder());
            this.maxDist = maxDist;
        }

        public boolean tryAddMember(@Nonnull Model model) {
            Vector3d pos = getEntityPos(model.holder());
            boolean inRange = center.distance(pos) <= maxDist;

            if (!inRange) {
                return false;
            }

            int memberNodeCount = getBlockyNodeCount(model.holder());

            boolean nodesExceedLimit = (memberNodeCount + blockyNodeCount) > (HytaleService.NODE_LIMIT - entities.size() - 1);

            if (nodesExceedLimit) {
                return false;
            }

            blockyNodeCount += memberNodeCount;

            entities.add(model);

            pos.sub(center, tmp);
            center.fma(1.0 / entities.size(), tmp);
            return true;
        }

        private int getBlockyNodeCount(Holder<EntityStore> holder) {
            Model model = blockymodelService.loadModelFromHolder(holder);
            BlockymodelBase blockymodelBase = blockymodelService.loadBlockymodelBase(model.path());
            int count = blockymodelService.countNodes(blockymodelBase);
            count += blockymodelService.countAttachmentsNodes(model);
            return count;
        }
    }
}
