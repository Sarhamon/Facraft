package io.github.sarhamon.facraft.machine;

import java.util.ArrayList;
import java.util.List;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 조립기 멀티블록: 3x3x2. 코어는 1층 가운데, 나머지 17칸은 프레임.
 *
 *  1층      2층
 * [프프프] [프프프]
 * [프코프] [프프프]
 * [프프프] [프프프]
 *
 * 프레임은 코어 기준 x,z ±1, y 0~1에 있음. 그래서 프레임에서 코어를 찾을 때는
 * 반대로 x,z ±1, y -1~0만 보면 되고, 형성된 조립기끼리는 프레임을 공유할 수 없으니 코어가 하나로 정해짐.
 */
public final class AssemblingMachineStructure {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private AssemblingMachineStructure() {
    }

    public static List<BlockPos> framePositions(BlockPos core) {
        List<BlockPos> positions = new ArrayList<>(17);
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        positions.add(core.offset(dx, dy, dz));
                    }
                }
            }
        }
        return positions;
    }

    // 형성된 구조 전체 테두리 (코어 기준, 픽셀 단위: 3x3 바닥, 높이 2블록)
    private static final VoxelShape FORMED_SHAPE = Block.box(-16, 0, -16, 32, 32, 32);

    // partPos 블록 기준으로 옮긴 전체 테두리
    public static VoxelShape formedShape(BlockPos partPos, BlockPos core) {
        return FORMED_SHAPE.move(core.getX() - partPos.getX(), core.getY() - partPos.getY(), core.getZ() - partPos.getZ());
    }

    // 프레임 위치에서, 형성 상태가 formed인 코어 후보들을 찾음
    public static List<BlockPos> findCores(BlockGetter level, BlockPos framePos, boolean formed) {
        List<BlockPos> cores = new ArrayList<>();
        for (int dy = -1; dy <= 0; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = framePos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Facraft.ASSEMBLING_MACHINE.get()) && state.getValue(FORMED) == formed) {
                        cores.add(pos);
                    }
                }
            }
        }
        return cores;
    }

    // 망치로 클릭한 위치(코어 또는 프레임)에서 형성 시도. 성공하면 true
    public static boolean tryForm(Level level, BlockPos clicked) {
        List<BlockPos> candidates = level.getBlockState(clicked).is(Facraft.ASSEMBLING_MACHINE.get())
                ? List.of(clicked)
                : findCores(level, clicked, false);
        for (BlockPos core : candidates) {
            if (isComplete(level, core)) {
                setFormed(level, core, true);
                return true;
            }
        }
        return false;
    }

    // 코어 주변 17칸이 전부 형성 안 된 프레임인지
    private static boolean isComplete(Level level, BlockPos core) {
        for (BlockPos pos : framePositions(core)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(Facraft.ASSEMBLING_MACHINE_FRAME.get()) || state.getValue(FORMED)) {
                return false;
            }
        }
        return true;
    }

    // 형성/해제. 구조 중 일부가 이미 부서진 상태에서도 남은 칸만 바꿈
    public static void setFormed(Level level, BlockPos core, boolean formed) {
        setFormedIfPart(level, core, Facraft.ASSEMBLING_MACHINE.get(), formed);
        for (BlockPos pos : framePositions(core)) {
            setFormedIfPart(level, pos, Facraft.ASSEMBLING_MACHINE_FRAME.get(), formed);
        }
    }

    private static void setFormedIfPart(Level level, BlockPos pos, Block block, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.is(block) && state.getValue(FORMED) != formed) {
            level.setBlock(pos, state.setValue(FORMED, formed), Block.UPDATE_ALL);
        }
    }
}
