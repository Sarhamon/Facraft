package io.github.sarhamon.facraft.belt;

import com.mojang.serialization.MapCodec;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// 운송 벨트 (노랑). FACING = 아이템이 흘러가는 방향
public class TransportBeltBlock extends BaseEntityBlock {
    public static final MapCodec<TransportBeltBlock> CODEC = simpleCodec(TransportBeltBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public TransportBeltBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 플레이어가 바라보는 방향으로 흐르게 설치
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransportBeltBlockEntity(pos, state);
    }

    // 손에 든 아이템 1개를 클릭한 레인, 클릭한 위치에 올림 (테스트용 투입 방법)
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty() || stack.is(Facraft.TRANSPORT_BELT_ITEM.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // 벨트를 들고 있으면 평소처럼 설치
        }
        if (level instanceof ServerLevel serverLevel) {
            Direction facing = state.getValue(FACING);
            Direction right = facing.getClockWise();
            Vec3 local = hitResult.getLocation().subtract(Vec3.atCenterOf(pos));
            double lateral = local.x * right.getStepX() + local.z * right.getStepZ();
            double along = local.x * facing.getStepX() + local.z * facing.getStepZ();
            int lane = lateral > 0 ? 1 : 0;
            int offset = Mth.clamp((int) Math.round((along + 0.5) * BeltLine.TILE_LENGTH), 0, BeltLine.TILE_LENGTH);
            if (BeltManager.get(serverLevel).insert(pos, lane, offset, stack.copyWithCount(1)) && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
