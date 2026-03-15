package dev.marggx.mcreator.data.blockymodel;

public enum BlockymodelShapeTextureLayoutKey {
    Top("top"),
    Bottom("bottom"),
    Front("front"),
    Back("back"),
    Left("left"),
    Right("right");

    public static final BlockymodelShapeTextureLayoutKey[] VALUES = values();
    private final String value;

    BlockymodelShapeTextureLayoutKey(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static BlockymodelShapeTextureLayoutKey fromValue(String value) {
        return switch (value) {
            case "top" -> Top;
            case "bottom" -> Bottom;
            case "front" -> Front;
            case "back" -> Back;
            case "left" -> Left;
            case "right" -> Right;
            case null -> null;
            default -> throw new RuntimeException("Invalid shading mode type: " + value);
        };
    }
}
