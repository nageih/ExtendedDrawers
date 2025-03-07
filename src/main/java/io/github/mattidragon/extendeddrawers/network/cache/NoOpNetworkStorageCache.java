package io.github.mattidragon.extendeddrawers.network.cache;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.LinkEntity;
import com.kneelawk.graphlib.api.graph.user.NodeEntity;
import com.kneelawk.graphlib.api.util.LinkPos;
import io.github.mattidragon.extendeddrawers.block.entity.StorageDrawerBlockEntity;
import io.github.mattidragon.extendeddrawers.storage.DrawerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class NoOpNetworkStorageCache implements NetworkStorageCache {
    private GraphEntityContext context;

    @Override
    public CombinedStorage<ItemVariant, DrawerStorage> get() {
        return new CombinedStorage<>(context.getGraph()
                .getNodes()
                .<DrawerStorage>flatMap(node -> {
                    if (node.getBlockEntity() instanceof StorageDrawerBlockEntity drawer) {
                        return drawer.streamStorages();
                    }
                    return Stream.empty();
                })
                .sorted()
                .toList());
    }

    @Override
    public void update() {

    }

    @Override
    public void forceUpdate() {

    }

    @Override
    public void onSortingChanged() {

    }

    @Override
    public void onNodeCreated(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity) {
        NetworkStorageCache.super.onNodeCreated(node, nodeEntity);
    }

    @Override
    public void onNodeDestroyed(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity, Map<LinkPos, LinkEntity> linkEntities) {
        NetworkStorageCache.super.onNodeDestroyed(node, nodeEntity, linkEntities);
    }

    @Override
    public void onNodeUnloaded(BlockPos pos) {

    }

    @Override
    public void onNodeReloaded(BlockPos pos) {

    }

    @Override
    public @NotNull NetworkStorageCache split(@NotNull BlockGraph originalGraph, @NotNull BlockGraph newGraph) {
        return new NoOpNetworkStorageCache();
    }

    @Override
    public List<Text> getDebugInfo() {
        var list = new ArrayList<Text>();
        list.add(Text.literal("No-Op Storage Cache Debug Info").formatted(Formatting.BOLD, Formatting.YELLOW));
        return list;
    }

    @Override
    public Text getDebugInfo(BlockPos pos) {
        return Text.literal("-");
    }

    @Override
    public void onInit(@NotNull GraphEntityContext ctx) {
        this.context = ctx;
    }

    @Override
    public @NotNull GraphEntityContext getContext() {
        return context;
    }

    @Override
    public void merge(@NotNull NetworkStorageCache other) {

    }
}
