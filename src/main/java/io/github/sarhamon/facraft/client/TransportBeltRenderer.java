package io.github.sarhamon.facraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.sarhamon.facraft.belt.BeltItem;
import io.github.sarhamon.facraft.belt.BeltShape;
import io.github.sarhamon.facraft.belt.TransportBeltBlock;
import io.github.sarhamon.facraft.belt.TransportBeltBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

// 벨트 위 아이템 그리기. 아이템은 엔티티가 아니라 이 렌더러가 칸마다 직접 그림
public class TransportBeltRenderer implements BlockEntityRenderer<TransportBeltBlockEntity> {
    private final ItemRenderer itemRenderer;

    public TransportBeltRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TransportBeltBlockEntity belt, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = belt.getBlockState();
        Direction facing = state.getValue(TransportBeltBlock.FACING);
        BeltShape shape = state.getValue(TransportBeltBlock.SHAPE);
        Direction right = facing.getClockWise();
        for (BeltItem item : belt.getItems()) {
            double t = item.offset() / 256.0;              // 0(입구) ~ 1(출구)
            double lateral = item.lane() == 0 ? -0.25 : 0.25; // 진행 방향 기준 왼쪽(-)/오른쪽(+)
            double x;
            double z;
            if (shape == BeltShape.STRAIGHT) {
                double along = t - 0.5;
                x = facing.getStepX() * along + right.getStepX() * lateral;
                z = facing.getStepZ() * along + right.getStepZ() * lateral;
            } else {
                // 코너: 입구 쪽 변(side)과 출구 쪽 변(facing)이 만나는 모서리를 중심으로 하는 사분원
                Direction side = shape.inputSide(facing);
                double cornerX = 0.5 * (side.getStepX() + facing.getStepX());
                double cornerZ = 0.5 * (side.getStepZ() + facing.getStepZ());
                double sideSign = side == right ? 1 : -1;
                double radius = 0.5 - lateral * sideSign; // 안쪽 레인은 반지름 0.25, 바깥쪽은 0.75
                double angle = t * Math.PI / 2;
                x = cornerX - radius * (facing.getStepX() * Math.cos(angle) + side.getStepX() * Math.sin(angle));
                z = cornerZ - radius * (facing.getStepZ() * Math.cos(angle) + side.getStepZ() * Math.sin(angle));
            }
            poseStack.pushPose();
            poseStack.translate(0.5 + x, 2.5 / 16, 0.5 + z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(90)); // 벨트 위에 눕힘
            poseStack.scale(0.4f, 0.4f, 0.4f);
            itemRenderer.renderStatic(item.stack(), ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffers,
                    belt.getLevel(), 0);
            poseStack.popPose();
        }
    }
}
