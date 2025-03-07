package io.github.mattidragon.extendeddrawers.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public class DrawerRaycastUtil {
    @Nullable
    public static Vec2f calculateFaceLocation(BlockPos blockPos, Vec3d hitPos, Direction hitDirection, Direction blockDirection) {
        if (hitDirection != blockDirection) return null;
        var internalPos = hitPos.subtract(Vec3d.of(blockPos));
        
        return switch (blockDirection) {
            case NORTH -> new Vec2f((float) (1 - internalPos.x), (float) (1 - internalPos.y));
            case SOUTH -> new Vec2f((float) (internalPos.x), (float) (1 - internalPos.y));
            case EAST -> new Vec2f((float) (1 - internalPos.z), (float) (1 - internalPos.y));
            case WEST -> new Vec2f((float) (internalPos.z), (float) (1 - internalPos.y));
            default -> null;
        };
    }
    
    public static BlockHitResult getTarget(PlayerEntity player, BlockPos target) {
        var from = player.getEyePos();
        var length = Vec3d.ofCenter(target).subtract(from).length() + 1; //Add a bit of extra length for consistency
        var looking = player.getRotationVector();
        var to = from.add(looking.multiply(length));
        return player.getWorld().raycast(new RaycastContext(from, to, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
    }
}
