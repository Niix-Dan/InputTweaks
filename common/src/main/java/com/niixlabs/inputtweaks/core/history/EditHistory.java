package com.niixlabs.inputtweaks.core.history;

import java.util.ArrayDeque;
import java.util.Deque;

public final class EditHistory {
    private static final int MAX_STEPS = 128;

    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();

    public void perform(EditCommand command) {
        command.execute().run();
        undoStack.push(command);
        if (undoStack.size() > MAX_STEPS) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        EditCommand cmd = undoStack.pop();
        cmd.undo().run();
        redoStack.push(cmd);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        EditCommand cmd = redoStack.pop();
        cmd.execute().run();
        undoStack.push(cmd);
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
