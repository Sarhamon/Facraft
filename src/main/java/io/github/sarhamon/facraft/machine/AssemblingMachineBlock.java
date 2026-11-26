package io.github.sarhamon.facraft.machine;

import static io.github.sarhamon.facraft.machine.AssemblingMachineStructure.FORMED;

import com.mojang.serialization.MapCodec;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// 조립기 코어. 멀티블록이 형성됐을 때만 GUI를 열고 제작함
public class AssemblingMachineBlock extends BaseEntityBlock {
    public static final MapCodec<AssemblingMachineBlock> CODEC = simpleCodec(AssemblingMachineBlock::new);

    public AssemblingMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    // BaseEntityBlock 기본값은 INVISIBLE이라 일반 블록 모델로 그리도록 바꿈
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // 형성되면 구조 전체(3x3x2) 테두리. 충돌 판정은 항상 1칸
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? AssemblingMachineStructure.formedShape(pos, pos) : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblingMachineBlockEntity(pos, state);
    }

    // 서버에서만 틱을 돌림. 클라이언트는 GUI에 동기화된 값만 그림
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, Facraft.ASSEMBLING_MACHINE_BLOCK_ENTITY.get(), AssemblingMachineBlockEntity::serverTick);
    }

    // 망치 처리는 여기서. 기본 상호작용(useWithoutItem)보다 먼저 호출됨
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Facraft.HAMMER.get()) && !state.getValue(FORMED)) {
            if (!level.isClientSide) {
                reportFormResult(player, AssemblingMachineStructure.tryForm(level, pos));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (!state.getValue(FORMED)) {
                player.displayClientMessage(Component.translatable("message.facraft.assembling_machine.not_formed"), true);
            } else if (level.getBlockEntity(pos) instanceof AssemblingMachineBlockEntity machine) {
                player.openMenu(machine);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static void reportFormResult(Player player, boolean formed) {
        player.displayClientMessage(Component.translatable(formed
                ? "message.facraft.assembling_machine.formed"
                : "message.facraft.assembling_machine.incomplete"), true);
    }

    // 블록이 부서지면 안에 든 아이템을 떨구고, 형성돼 있었으면 프레임들을 해제
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof AssemblingMachineBlockEntity machine) {
                machine.dropContents(level, pos);
            }
            if (state.getValue(FORMED)) {
                AssemblingMachineStructure.setFormed(level, pos, false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
