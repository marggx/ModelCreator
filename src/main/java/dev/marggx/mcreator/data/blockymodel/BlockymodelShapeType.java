package dev.marggx.mcreator.data.blockymodel;


public enum BlockymodelShapeType {
    Box("box"),
    None("none"),
    Quad("quad");

    public static final BlockymodelShapeType[] VALUES = values();
    private final String value;

    BlockymodelShapeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static BlockymodelShapeType fromValue(String value) {
        return switch (value) {
            case "box" -> Box;
            case "none" -> None;
            case "quad" -> Quad;
            case null -> null;
            default -> throw new RuntimeException("Invalid bench type: " + value);
        };
    }
}
