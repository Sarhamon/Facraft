package io.github.sarhamon.facraft.machine;

import static io.github.sarhamon.facraft.machine.AssemblingMachineStructure.FORMED;

import java.util.List;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// 조립기 프레임. BlockEntity 없이 형성 여부만 블록 상태로 가짐
public class AssemblingMachineFrameBlock extends Block {
    public AssemblingMachineFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    // 형성되면 프레임은 숨기고 코어가 3x3x2 모델을 그림. 충돌 판정은 그대로
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    // 형성되면 마우스를 올렸을 때 구조 전체(3x3x2) 테두리를 보여 줌
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(FORMED)) {
            List<BlockPos> cores = AssemblingMachineStructure.findCores(level, pos, true);
            if (!cores.isEmpty()) {
                return AssemblingMachineStructure.formedShape(pos, cores.get(0));
            }
        }
        return Shapes.block();
    }

    // 충돌 판정은 항상 1칸 (기본값은 getShape를 따라가므로 따로 지정)
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    // 망치 처리는 여기서. 기본 상호작용(useWithoutItem)보다 먼저 호출됨
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Facraft.HAMMER.get()) && !state.getValue(FORMED)) {
            if (!level.isClientSide) {
                AssemblingMachineBlock.reportFormResult(player, AssemblingMachineStructure.tryForm(level, pos));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // 형성된 프레임을 우클릭하면 코어의 GUI를 엶
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            List<BlockPos> cores = AssemblingMachineStructure.findCores(level, pos, true);
            if (!cores.isEmpty() && level.getBlockEntity(cores.get(0)) instanceof AssemblingMachineBlockEntity machine) {
                player.openMenu(machine);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // 형성된 프레임이 부서지면 구조 전체를 해제
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getValue(FORMED) && !state.is(newState.getBlock())) {
            for (BlockPos core : AssemblingMachineStructure.findCores(level, pos, true)) {
                AssemblingMachineStructure.setFormed(level, core, false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
