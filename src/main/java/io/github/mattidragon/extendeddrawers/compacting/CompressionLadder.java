package io.github.mattidragon.extendeddrawers.compacting;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import java.util.List;

/**
 * Represents a bidirectional ladder of compression recipes.
 * First step has the base item (e.g. nuggets) and a size of 1.
 * Each step after that has a new item (ingots, blocks) and a size that counts how many of the first tier is necessary (9, 81).
 */
@SuppressWarnings("UnstableApiUsage")
public record CompressionLadder(List<Step> steps) {
    public CompressionLadder(List<Step> steps) {
        this.steps = List.copyOf(steps);
    }

    /**
     * @param item The item at this step.
     * @param size The amount of the first step required to craft this one.
     */
    public record Step(ItemVariant item, int size) {
    }

    public int getPosition(ItemVariant item) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).item.equals(item))
                return i;
        }
        return -1;
    }
}
