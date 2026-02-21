package dev.marggx.mcomb.data.blockymodel;


public enum BlockymodelShapeShadingMode {
    Flat("flat"),
    Standard("standard"),
    Fullbright("fullbright"),
    Reflective("reflective");

    public static final BlockymodelShapeShadingMode[] VALUES = values();
    private final String value;

    BlockymodelShapeShadingMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static BlockymodelShapeShadingMode fromValue(String value) {
        return switch (value) {
            case "flat" -> Flat;
            case "standard" -> Standard;
            case "fullbright" -> Fullbright;
            case "reflective" -> Reflective;
            case null -> null;
            default -> throw new RuntimeException("Invalid shading mode type: " + value);
        };
    }
}
