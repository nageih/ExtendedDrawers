package io.github.mattidragon.extendeddrawers.block;

import io.github.mattidragon.extendeddrawers.ExtendedDrawers;
import io.github.mattidragon.extendeddrawers.block.base.CreativeBreakBlocker;
import io.github.mattidragon.extendeddrawers.block.base.DrawerInteractionHandler;
import io.github.mattidragon.extendeddrawers.block.base.NetworkBlockWithEntity;
import io.github.mattidragon.extendeddrawers.block.entity.DrawerBlockEntity;
import io.github.mattidragon.extendeddrawers.item.UpgradeItem;
import io.github.mattidragon.extendeddrawers.misc.DrawerInteractionStatusManager;
import io.github.mattidragon.extendeddrawers.misc.DrawerRaycastUtil;
import io.github.mattidragon.extendeddrawers.misc.ItemUtils;
import io.github.mattidragon.extendeddrawers.network.node.DrawerBlockNode;
import io.github.mattidragon.extendeddrawers.network.node.DrawerNetworkBlockNode;
import io.github.mattidragon.extendeddrawers.registry.ModBlocks;
import io.github.mattidragon.extendeddrawers.registry.ModItems;
import io.github.mattidragon.extendeddrawers.storage.DrawerSlot;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"UnstableApiUsage", "deprecation"}) // transfer api and mojank block method deprecation
public class DrawerBlock extends NetworkBlockWithEntity<DrawerBlockEntity> implements DrawerInteractionHandler, CreativeBreakBlocker {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public final int slots;

    public DrawerBlock(Settings settings, int slots) {
        super(settings);
        this.slots = slots;
        setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView blockView, List<Text> tooltip, TooltipContext context) {
        var nbt = BlockItem.getBlockEntityNbt(stack);
        if (nbt == null) return;

        var list = nbt.getList("items", NbtElement.COMPOUND_TYPE).stream()
                .map(NbtCompound.class::cast)
                .map(data -> {
                    var slot = new DrawerSlot(null, 1);
                    slot.readNbt(data);
                    return slot;
                })
                .filter(slot -> !slot.isBlank() || slot.getUpgrade() != null || slot.isHidden() || slot.isLocked() || slot.isVoiding())
                .toList();
        if (list.isEmpty()) return;
        boolean shift = ExtendedDrawers.SHIFT_ACCESS.isShiftPressed();

        if (!shift) {
            tooltip.add(Text.translatable("tooltip.extended_drawers.shift_for_modifiers").formatted(Formatting.GRAY));
            tooltip.add(Text.empty());
        }

        if (!list.stream().allMatch(DrawerSlot::isBlank) || shift)
            tooltip.add(Text.translatable("tooltip.extended_drawers.drawer_contents").formatted(Formatting.GRAY));
        for (var slot : list) {
            MutableText text;
            if (!slot.isBlank()) {
                text = Text.literal(" - ");
                text.append(Text.literal(String.valueOf(slot.getTrueAmount())))
                        .append(" ")
                        .append(slot.getResource().toStack().getName());
            } else if (shift) {
                text = Text.literal(" - ");
                text.append(Text.translatable("tooltip.extended_drawers.empty").formatted(Formatting.ITALIC));
            } else continue;

            // Seems like client code is safe here. If this breaks then other mods are broken too.
            if (shift) {
                text.append("  ")
                        .append(Text.literal("V").formatted(slot.isVoiding() ? Formatting.WHITE : Formatting.DARK_GRAY))
                        .append(Text.literal("L").formatted(slot.isLocked() ? Formatting.WHITE : Formatting.DARK_GRAY))
                        .append(Text.literal("H").formatted(slot.isHidden() ? Formatting.WHITE : Formatting.DARK_GRAY));
                if (slot.isDuping())
                    text.append(Text.literal("D").formatted(Formatting.WHITE));

                if (slot.getUpgrade() != null) {
                    text.append(" ").append(slot.getUpgrade().getName().copy().formatted(Formatting.AQUA));
                }
            }
            tooltip.add(text.formatted(Formatting.GRAY));
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            var drawer = getBlockEntity(world, pos);
            if (drawer != null && ExtendedDrawers.CONFIG.get().misc().drawersDropContentsOnBreak()) {
                for (var slot : drawer.storages) {
                    ItemUtils.offerOrDropStacks(world, pos, null, null, slot.getItem(), slot.getAmount());
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }
    
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
    
    @Override
    protected BlockEntityType<DrawerBlockEntity> getType() {
        return ModBlocks.DRAWER_BLOCK_ENTITY;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hit.getSide() != state.get(FACING) || !player.canModifyBlocks() || hand == Hand.OFF_HAND)
            return ActionResult.PASS;
        if (!(world instanceof ServerWorld)) return ActionResult.CONSUME_PARTIAL;
    
        var internalPos = DrawerRaycastUtil.calculateFaceLocation(pos, hit.getPos(), hit.getSide(), state.get(FACING));
        if (internalPos == null) return ActionResult.PASS;
        var slot = getSlot(internalPos);
        
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return ActionResult.PASS;
        var playerStack = player.getStackInHand(hand);
        var storage = drawer.storages[slot];

        // Upgrade & limiter removal
        if (playerStack.isEmpty() && player.isSneaking()) {
            // remove limiter first, if that fails, remove upgrade
            var changeResult = storage.changeLimiter(ItemVariant.blank(), world, pos, hit.getSide(), player)
                               || storage.changeUpgrade(ItemVariant.blank(), world, pos, hit.getSide(), player);
            return changeResult ? ActionResult.SUCCESS : ActionResult.FAIL;
        }
    
        var isDoubleClick = DrawerInteractionStatusManager.getAndResetInsertStatus(player, pos, slot);
    
        try (var t = Transaction.openOuter()) {
            int inserted;

            storage.overrideLock(t);
            if (isDoubleClick) {
                if (storage.isResourceBlank()) return ActionResult.PASS;
                inserted = (int) StorageUtil.move(PlayerInventoryStorage.of(player), storage, itemVariant -> true, Long.MAX_VALUE, t);
            } else {
                if (playerStack.isEmpty()) return ActionResult.PASS;
                
                inserted = (int) storage.insert(ItemVariant.of(playerStack), playerStack.getCount(), t);
                playerStack.decrement(inserted);
            }
            if (inserted == 0) return ActionResult.CONSUME_PARTIAL;
            
            t.commit();
            return ActionResult.CONSUME;
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        var blockEntity = getBlockEntity(world, pos);
        if (blockEntity != null && ExtendedDrawers.CONFIG.get().misc().dropDrawersInCreative() && !world.isClient && player.isCreative() && !blockEntity.isEmpty()) {
            getDroppedStacks(state, (ServerWorld) world, pos, blockEntity, player, player.getStackInHand(Hand.MAIN_HAND))
                    .forEach(stack -> ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack));
        }

        super.onBreak(world, pos, state, player);
    }
    
    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!player.canModifyBlocks()) return;
        
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return;
        
        // We don't have sub-block position or a hit result, so we need to raycast ourselves
        var hit = DrawerRaycastUtil.getTarget(player, pos);
        if (hit.getType() == HitResult.Type.MISS) return;
        var internalPos = DrawerRaycastUtil.calculateFaceLocation(pos, hit.getPos(), hit.getSide(), state.get(FACING));
        if (internalPos == null) return;
    
        var slot = getSlot(internalPos);
        var storage = drawer.storages[slot];
        if (storage.isResourceBlank()) return;
        
        try (var t = Transaction.openOuter()) {
            var item = storage.getItem(); // cache because it changes
            var extracted = (int) storage.extract(item, player.isSneaking() ? item.getItem().getMaxCount() : 1, t);
            if (extracted == 0) return;
    
            player.getInventory().offerOrDrop(item.toStack(extracted));
            
            t.commit();
        }
    }
    
    private int getSlot(Vec2f facePos) {
        return switch (slots) {
            case 1 -> 0;
            case 2 -> facePos.x < 0.5f ? 0 : 1;
            case 4 -> facePos.y < 0.5f ? facePos.x < 0.5f ? 0 : 1 : facePos.x < 0.5f ? 2 : 3;
            default -> throw new IllegalStateException("unexpected drawer slot count");
        };
    }
    
    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }
    
    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return 0;
        return StorageUtil.calculateComparatorOutput(drawer.combinedStorage);
    }
    
    @Override
    public ActionResult toggleLock(BlockState state, World world, BlockPos pos, Vec3d hitPos, Direction side) {
        var facePos = DrawerRaycastUtil.calculateFaceLocation(pos, hitPos, side, state.get(FACING));
        if (facePos == null) return ActionResult.PASS;
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return ActionResult.PASS;
        var storage = drawer.storages[getSlot(facePos)];
        storage.setLocked(!storage.isLocked());
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult toggleVoid(BlockState state, World world, BlockPos pos, Vec3d hitPos, Direction side) {
        var facePos = DrawerRaycastUtil.calculateFaceLocation(pos, hitPos, side, state.get(FACING));
        if (facePos == null) return ActionResult.PASS;
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return ActionResult.PASS;
        var storage = drawer.storages[getSlot(facePos)];
        storage.setVoiding(!storage.isVoiding());
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult toggleHide(BlockState state, World world, BlockPos pos, Vec3d hitPos, Direction side) {
        var facePos = DrawerRaycastUtil.calculateFaceLocation(pos, hitPos, side, state.get(FACING));
        if (facePos == null) return ActionResult.PASS;
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return ActionResult.PASS;
        var storage = drawer.storages[getSlot(facePos)];
        storage.setHidden(!storage.isHidden());
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult toggleDuping(BlockState state, World world, BlockPos pos, Vec3d hitPos, Direction side) {
        var facePos = DrawerRaycastUtil.calculateFaceLocation(pos, hitPos, side, state.get(FACING));
        if (facePos == null) return ActionResult.PASS;
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return ActionResult.PASS;
        var storage = drawer.storages[getSlot(facePos)];
        storage.setDuping(!storage.isDuping());
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult changeUpgrade(BlockState state, World world, BlockPos pos, Vec3d hitPos, Direction side, PlayerEntity player, ItemStack stack) {
        if (world.isClient) return ActionResult.SUCCESS;
       
        var facePos = DrawerRaycastUtil.calculateFaceLocation(pos, hitPos, side, state.get(FACING));
        if (facePos == null) return ActionResult.PASS;
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return ActionResult.PASS;
        var storage = drawer.storages[getSlot(facePos)];
    
        if (!(stack.getItem() instanceof UpgradeItem)) {
            ExtendedDrawers.LOGGER.warn("Expected drawer upgrade to be UpgradeItem but found " + stack.getItem().getClass().getSimpleName() + " instead");
            return ActionResult.FAIL;
        }

        var changed = storage.changeUpgrade(ItemVariant.of(stack), world, pos, side, player);
        if (changed)
            stack.decrement(1);

        return changed ? ActionResult.SUCCESS : ActionResult.FAIL;
    }

    @Override
    public ActionResult changeLimiter(BlockState state, World world, BlockPos pos, Vec3d hitPos, Direction side, PlayerEntity player, ItemStack stack) {
        if (world.isClient) return ActionResult.SUCCESS;

        var facePos = DrawerRaycastUtil.calculateFaceLocation(pos, hitPos, side, state.get(FACING));
        if (facePos == null) return ActionResult.PASS;
        var drawer = getBlockEntity(world, pos);
        if (drawer == null) return ActionResult.PASS;
        var storage = drawer.storages[getSlot(facePos)];

        if (!stack.isOf(ModItems.LIMITER)) {
            ExtendedDrawers.LOGGER.warn("Expected limiter to be limiter but found " + stack + " instead");
            return ActionResult.FAIL;
        }

        var changed = storage.changeLimiter(ItemVariant.of(stack), world, pos, side, player);
        if (changed) {
            stack.decrement(1);
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }
    
    @Override
    public boolean isFront(BlockState state, Direction direction) {
        return state.get(FACING) == direction;
    }
    
    @Override
    public DrawerNetworkBlockNode getNode() {
        return DrawerBlockNode.INSTANCE;
    }
}
