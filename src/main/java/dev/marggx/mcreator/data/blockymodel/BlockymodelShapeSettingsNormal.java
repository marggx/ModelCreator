package dev.marggx.mcreator.data.blockymodel;


public enum BlockymodelShapeSettingsNormal {
    PX("+X"),
    PY("+Y"),
    PZ("+Z"),
    NX("-X"),
    NY("-Y"),
    NZ("-Z");

    public static final BlockymodelShapeSettingsNormal[] VALUES = values();
    private final String value;

    BlockymodelShapeSettingsNormal(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static BlockymodelShapeSettingsNormal fromValue(String value) {
        return switch (value) {
            case "+X" -> PX;
            case "+Y" -> PY;
            case "+Z" -> PZ;
            case "-X" -> NX;
            case "-Y" -> NY;
            case "-Z" -> NZ;
            case null -> null;
            default -> throw new RuntimeException("Invalid bench type: " + value);
        };
    }
}
