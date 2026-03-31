package dev.marggx.mcreator.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.marggx.mcreator.utils.Logger;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class EditPage extends InteractiveCustomUIPage<EditPage.PageData> {
    private static final Logger LOGGER = Logger.get();

    private Ref<EntityStore> entityRef;
    private TransformComponent originalTransform;
    private TransformComponent transform;
    private HeadRotation headRotation;
    private HeadRotation originalHeadRotation;
    private EntityScaleComponent scale;
    private EntityScaleComponent originalScale;

    public EditPage(@Nonnull PlayerRef playerRef, Ref<EntityStore> entityRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.entityRef = entityRef;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cBuilder, @NonNullDecl UIEventBuilder eBuilder, @NonNullDecl Store<EntityStore> store) {
        cBuilder.append("Pages/Edit.ui");

        UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
        transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        headRotation = store.getComponent(entityRef, HeadRotation.getComponentType());
        ModelComponent model = store.getComponent(entityRef, ModelComponent.getComponentType());
        BlockEntity blockEntity = store.getComponent(entityRef, BlockEntity.getComponentType());
        scale = store.getComponent(entityRef, EntityScaleComponent.getComponentType());
        ItemComponent itemComponent = store.getComponent(entityRef, ItemComponent.getComponentType());

        Archetype<EntityStore> arch = store.getArchetype(entityRef);
        String name = itemComponent != null ? itemComponent.getItemStack().getItemId() : model != null ? model.getModel().getModelAssetId() : blockEntity != null ? blockEntity.getBlockTypeKey() : "Entity";
        cBuilder.set("#Label.Text", name);

        if (uuidComponent != null) {
            cBuilder.set("#Uuid #Label.Text", uuidComponent.getUuid().toString());
        }

        if (model != null) {
            cBuilder.set("#Model.Visible", true);
            cBuilder.set("#Model #Label.Text", model.getModel().getModelAssetId());
        }

        if (itemComponent != null) {
            cBuilder.set("#Item.Visible", true);
            cBuilder.set("#Item #Label.Text", itemComponent.getItemStack().getItemId());
        }

        if (transform != null) {
            originalTransform = transform.clone();
            Vector3d pos = transform.getPosition();
            buildValueField(cBuilder, eBuilder, PageData.Selector.PosX, PageData.InputType.Float, String.valueOf(MathUtil.round(pos.x, 2)), true);
            buildValueField(cBuilder, eBuilder, PageData.Selector.PosY, PageData.InputType.Float, String.valueOf(MathUtil.round(pos.y, 2)), true);
            buildValueField(cBuilder, eBuilder, PageData.Selector.PosZ, PageData.InputType.Float, String.valueOf(MathUtil.round(pos.z, 2)), true);

            if (model != null) {
                cBuilder.set("#Rot.Visible", true);
                Vector3f rot = transform.getRotation();
                buildValueField(cBuilder, eBuilder, PageData.Selector.RotPitch, PageData.InputType.Float, String.valueOf(MathUtil.round(rot.x, 2)));
                buildValueField(cBuilder, eBuilder, PageData.Selector.RotYaw, PageData.InputType.Float, String.valueOf(MathUtil.round(rot.y, 2)));
                buildValueField(cBuilder, eBuilder, PageData.Selector.RotRoll, PageData.InputType.Float, String.valueOf(MathUtil.round(rot.z, 2)));
            }
        }

        if (headRotation != null) {
            originalHeadRotation = headRotation.clone();
            Vector3f rot = headRotation.getRotation();
            cBuilder.set("#HeadRot.Visible", true);
            buildValueField(cBuilder, eBuilder, PageData.Selector.HeadRotPitch, PageData.InputType.Float, String.valueOf(MathUtil.round(rot.x, 2)));
            buildValueField(cBuilder, eBuilder, PageData.Selector.HeadRotYaw, PageData.InputType.Float, String.valueOf(MathUtil.round(rot.y, 2)));
            buildValueField(cBuilder, eBuilder, PageData.Selector.HeadRotRoll, PageData.InputType.Float, String.valueOf(MathUtil.round(rot.z, 2)));
        }

        if (scale != null) {
            originalScale = (EntityScaleComponent) scale.clone();
            cBuilder.set("#Scale.Visible", true);
            buildValueField(cBuilder, eBuilder, PageData.Selector.Scale, PageData.InputType.Float, String.valueOf(MathUtil.round(scale.getScale(), 2)));
        }


        eBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#Buttons #Save",
                new EventData().append(PageData.ACTION, PageData.Action.Save.name())
        );
        eBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#Buttons #Cancel",
                new EventData().append(PageData.ACTION, PageData.Action.Cancel.name())
        );
    }

    private void buildValueField(UICommandBuilder cBuilder, @NonNullDecl UIEventBuilder eBuilder, PageData.Selector selector, PageData.InputType type, String value) {
        buildValueField(cBuilder, eBuilder, selector, type, value, false);
    }
    private void buildValueField(UICommandBuilder cBuilder, @NonNullDecl UIEventBuilder eBuilder, PageData.Selector selector, PageData.InputType type, String value, boolean useFocusLost) {
        cBuilder.set(selector.getValue(), value);
        eBuilder.addEventBinding(
                useFocusLost ? CustomUIEventBindingType.FocusLost : CustomUIEventBindingType.ValueChanged, removeValueFromString(selector.getValue()),
                new EventData().append(PageData.ACTION, PageData.Action.ValueChanged)
                        .append(PageData.SELECTOR, selector.getValue())
                        .append(PageData.VALUE, selector.getValue())
                        .append(PageData.INPUT_TYPE, type)
        );
    }

    private String removeValueFromString(String selector) {
        return selector.endsWith(".Value") ? selector.substring(0, selector.length() - ".Value".length()) : selector;
    }


    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull EditPage.PageData data) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
        assert playerRefComponent != null;

        UICommandBuilder cBuilder = new UICommandBuilder();
        if (data.inputType != null) {
            handleInputType(cBuilder, data);
        }
        switch (data.action) {
            case Save: {
                playerComponent.getPageManager().setPage(ref, store, Page.None);
                return;
            }

            case Cancel: {
                if (originalTransform != null) {
                    transform.setPosition(originalTransform.getPosition());
                    transform.setRotation(originalTransform.getRotation());
                }
                if (originalHeadRotation != null) {
                    headRotation.setRotation(originalHeadRotation.getRotation());
                }
                if (originalScale != null) {
                    scale.setScale(originalScale.getScale());
                }
                playerComponent.getPageManager().setPage(ref, store, Page.None);
                return;
            }

            case ValueChanged: {
                handleValueChanged(cBuilder, data);
                break;
            }
        }
        this.sendUpdate(cBuilder);
    }

    private void handleValueChanged(@Nonnull UICommandBuilder cBuilder, @Nonnull EditPage.PageData data) {
        if (data.value.isBlank()) {
            return;
        }
        switch (data.selector) {
            case PosX:
                transform.getPosition().setX(Float.parseFloat(data.value));
                break;
            case PosY:
                transform.getPosition().setY(Float.parseFloat(data.value));
                break;
            case PosZ:
                transform.getPosition().setZ(Float.parseFloat(data.value));
                break;
            case RotPitch:
                transform.getRotation().setX(Float.parseFloat(data.value));
                break;
            case RotYaw:
                transform.getRotation().setY(Float.parseFloat(data.value));
                break;
            case RotRoll:
                transform.getRotation().setZ(Float.parseFloat(data.value));
                break;
            case HeadRotPitch:
                headRotation.getRotation().setX(Float.parseFloat(data.value));
                break;
            case HeadRotYaw:
                headRotation.getRotation().setY(Float.parseFloat(data.value));
                break;
            case HeadRotRoll:
                headRotation.getRotation().setZ(Float.parseFloat(data.value));
                break;
            case Scale:
                scale.setScale(Float.parseFloat(data.value));
                break;
        }
    }

    private void handleInputType(UICommandBuilder cBuilder, EditPage.PageData data) {
        String sanitized = floatInputConverter(data.value);
        cBuilder.set(data.selector.getValue(), sanitized);
        data.value = sanitized;
    }

    private String floatInputConverter(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        StringBuilder sb = floatStringBuilder(raw);
        String sanitized = sb.toString();
        if (sanitized.isEmpty() || sanitized.equals("-") ||
                sanitized.equals(".") || sanitized.equals("-.")) {
            return "";
        }

        return sanitized;
    }

    @NonNullDecl
    private static StringBuilder floatStringBuilder(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        boolean hasDot  = false;
        boolean hasMinus = false;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);

            if (c == '-' && !hasMinus && sb.isEmpty()) {
                hasMinus = true;
                sb.append(c);
            } else if (c == '.' && !hasDot) {
                hasDot = true;
                sb.append(c);
            } else if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb;
    }


    public static class PageData {
        public enum Action {
            Save,
            Cancel,
            ValueChanged;

            Action() {
            }
        }

        public enum InputType {
            Float;

            InputType() {}
        }

        public enum Selector {
            PosX("#Pos #X #Input.Value"),
            PosY("#Pos #Y #Input.Value"),
            PosZ("#Pos #Z #Input.Value"),
            RotPitch("#Rot #Pitch #Input.Value"),
            RotYaw("#Rot #Yaw #Input.Value"),
            RotRoll("#Rot #Roll #Input.Value"),
            HeadRotPitch("#HeadRot #Pitch #Input.Value"),
            HeadRotYaw("#HeadRot #Yaw #Input.Value"),
            HeadRotRoll("#HeadRot #Roll #Input.Value"),
            Scale("#Scale #Input.Value");

            private final String value;
            Selector(final String value) {
                this.value = value;
            }
            public String getValue() {
                return value;
            }
            public static Selector fromValue(String value) {
                return switch (value) {
                    case "#Pos #X #Input.Value" -> PosX;
                    case "#Pos #Y #Input.Value" -> PosY;
                    case "#Pos #Z #Input.Value" -> PosZ;
                    case "#Rot #Pitch #Input.Value" -> RotPitch;
                    case "#Rot #Yaw #Input.Value" -> RotYaw;
                    case "#Rot #Roll #Input.Value" -> RotRoll;
                    case "#HeadRot #Pitch #Input.Value" -> HeadRotPitch;
                    case "#HeadRot #Yaw #Input.Value" -> HeadRotYaw;
                    case "#HeadRot #Roll #Input.Value" -> HeadRotRoll;
                    case "#Scale #Input.Value" -> Scale;
                    default -> throw new RuntimeException("Invalid selector: " + value);
                };
            }
        }


        public static final String ACTION = "Action";
        public static final String INPUT_TYPE = "InputType";
        public static final String SELECTOR = "Selector";
        public static final String VALUE = "@Value";
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(
                        new KeyedCodec<>(ACTION, new EnumCodec<>(PageData.Action.class, EnumCodec.EnumStyle.LEGACY)),
                        (o, action) -> o.action = action,
                        o -> o.action
                )
                .add()
                .append(
                        new KeyedCodec<>(INPUT_TYPE, new EnumCodec<>(PageData.InputType.class, EnumCodec.EnumStyle.LEGACY)),
                        (o, inputType) -> o.inputType = inputType,
                        o -> o.inputType
                )
                .add()
                .append(new KeyedCodec<>(SELECTOR, Codec.STRING), (o, selector) -> o.selector = Selector.fromValue(selector), o -> o.selector.getValue())
                .add()
                .append(new KeyedCodec<>(VALUE, Codec.STRING), (o, value) -> o.value = value, o -> o.value)
                .add()
                .build();
        public Action action;
        public InputType inputType;
        public Selector selector;
        public String value;

        public PageData() {
        }
    }
}