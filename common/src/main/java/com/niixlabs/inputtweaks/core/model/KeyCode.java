package com.niixlabs.inputtweaks.core.model;

public record KeyCode(InputType type, int code) implements Comparable<KeyCode> {
    public static final KeyCode UNKNOWN = new KeyCode(InputType.KEYBOARD, -1);

    public static KeyCode keyboard(int glfwKey) {
        return glfwKey < 0 ? UNKNOWN : new KeyCode(InputType.KEYBOARD, glfwKey);
    }

    public static KeyCode mouse(int button) {
        return button < 0 ? UNKNOWN : new KeyCode(InputType.MOUSE, button);
    }

    public static KeyCode parse(String raw) {
        if (raw == null || raw.isBlank() || "unbound".equalsIgnoreCase(raw)) {
            return UNKNOWN;
        }
        try {
            if (raw.startsWith("mouse:")) {
                return mouse(Integer.parseInt(raw.substring(6)));
            }
            if (raw.startsWith("key:")) {
                return keyboard(Integer.parseInt(raw.substring(4)));
            }
        } catch (NumberFormatException ignored) {
        }
        return UNKNOWN;
    }

    public boolean isUnknown() {
        return code < 0;
    }

    public String serialize() {
        if (isUnknown()) return "unbound";
        return (type == InputType.MOUSE ? "mouse:" : "key:") + code;
    }

    @Override
    public int compareTo(KeyCode other) {
        int cmp = this.type.compareTo(other.type);
        return cmp != 0 ? cmp : Integer.compare(this.code, other.code);
    }
}
