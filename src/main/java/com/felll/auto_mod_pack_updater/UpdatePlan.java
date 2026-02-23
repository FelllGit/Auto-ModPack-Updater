package com.felll.auto_mod_pack_updater;

import java.util.ArrayList;
import java.util.List;

public final class UpdatePlan {

    private final List<String> toAdd = new ArrayList<>();
    private final List<String> toUpdate = new ArrayList<>();
    private final List<String> toRemove = new ArrayList<>();

    public List<String> getToAdd() {
        return toAdd;
    }

    public List<String> getToUpdate() {
        return toUpdate;
    }

    public List<String> getToRemove() {
        return toRemove;
    }

    public boolean hasChanges() {
        return !toAdd.isEmpty() || !toUpdate.isEmpty() || !toRemove.isEmpty();
    }
}
