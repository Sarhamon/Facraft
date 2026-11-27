package io.github.sarhamon.facraft.belt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 벨트 모양. LEFT/RIGHT = 아이템이 (진행 방향 기준) 왼쪽/오른쪽 옆에서 들어와 꺾이는 코너.
 * 출구는 항상 FACING 방향.
 */
public enum BeltShape implements StringRepresentable {
    STRAIGHT("straight"),
    LEFT("left"),
    RIGHT("right");

    private final String name;

    BeltShape(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    // 아이템이 들어오는 쪽 칸의 방향 (이 칸 기준)
    public Direction inputSide(Direction facing) {
        return switch (this) {
            case STRAIGHT -> facing.getOpposite();
            case LEFT -> facing.getCounterClockWise();
            case RIGHT -> facing.getClockWise();
        };
    }

    /**
     * 주변 벨트를 보고 모양을 정함 (팩토리오 규칙).
     * 뒤에서 들어오면 직선(옆은 사이드 로딩), 옆 한쪽에서만 들어오면 코너, 그 외는 직선.
     */
    public static BeltShape compute(BlockGetter level, BlockPos pos, Direction facing) {
        if (feedsInto(level, pos, facing.getOpposite())) {
            return STRAIGHT;
        }
        boolean left = feedsInto(level, pos, facing.getCounterClockWise());
        boolean right = feedsInto(level, pos, facing.getClockWise());
        if (left && !right) {
            return LEFT;
        }
        if (right && !left) {
            return RIGHT;
        }
        return STRAIGHT;
    }

    // side 방향 이웃 벨트가 이 칸 쪽으로 아이템을 내보내는지
    private static boolean feedsInto(BlockGetter level, BlockPos pos, Direction side) {
        BlockState neighbor = level.getBlockState(pos.relative(side));
        return neighbor.getBlock() instanceof TransportBeltBlock
                && neighbor.getValue(TransportBeltBlock.FACING) == side.getOpposite();
    }
}
