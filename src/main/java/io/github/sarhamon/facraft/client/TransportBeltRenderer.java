package io.github.sarhamon.facraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.sarhamon.facraft.belt.BeltItem;
import io.github.sarhamon.facraft.belt.TransportBeltBlock;
import io.github.sarhamon.facraft.belt.TransportBeltBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

// 벨트 위 아이템 그리기. 아이템은 엔티티가 아니라 이 렌더러가 칸마다 직접 그림
public class TransportBeltRenderer implements BlockEntityRenderer<TransportBeltBlockEntity> {
    private final ItemRenderer itemRenderer;

    public TransportBeltRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TransportBeltBlockEntity belt, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Direction facing = belt.getBlockState().getValue(TransportBeltBlock.FACING);
        Direction right = facing.getClockWise();
        for (BeltItem item : belt.getItems()) {
            double along = item.offset() / 256.0 - 0.5;       // -0.5(뒤 끝) ~ 0.5(앞 끝)
            double lateral = item.lane() == 0 ? -0.25 : 0.25; // 왼쪽/오른쪽 레인
            poseStack.pushPose();
            poseStack.translate(
                    0.5 + facing.getStepX() * along + right.getStepX() * lateral,
                    2.5 / 16,
                    0.5 + facing.getStepZ() * along + right.getStepZ() * lateral);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(90)); // 벨트 위에 눕힘
            poseStack.scale(0.4f, 0.4f, 0.4f);
            itemRenderer.renderStatic(item.stack(), ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffers,
                    belt.getLevel(), 0);
            poseStack.popPose();
        }
    }
}
