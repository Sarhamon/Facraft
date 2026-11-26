package io.github.sarhamon.facraft.client;

import io.github.sarhamon.facraft.machine.AssemblingMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

// 임시 GUI: 텍스처 없이 사각형으로 배경과 슬롯 칸을 그림
public class AssemblingMachineScreen extends AbstractContainerScreen<AssemblingMachineMenu> {
    public AssemblingMachineScreen(AssemblingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF555555);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        }

        // 진행 바: 입력 칸과 출력 칸 사이
        int barX = leftPos + 102;
        int barY = topPos + 39;
        graphics.fill(barX, barY, barX + 26, barY + 8, 0xFF8B8B8B);
        graphics.fill(barX, barY, barX + menu.getScaledProgress(26), barY + 8, 0xFFE6B428);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
