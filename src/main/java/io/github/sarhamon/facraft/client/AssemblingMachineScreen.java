package io.github.sarhamon.facraft.client;

import java.util.List;

import io.github.sarhamon.facraft.machine.AssemblingMachineMenu;
import io.github.sarhamon.facraft.recipe.AssemblingRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;

// 임시 GUI: 텍스처 없이 사각형으로 배경과 슬롯 칸을 그림
public class AssemblingMachineScreen extends AbstractContainerScreen<AssemblingMachineMenu> {
    // 레시피 버튼 줄: 제목 아래, 기계 슬롯 위
    private static final int RECIPE_X = 8;
    private static final int RECIPE_Y = 18;
    private static final int RECIPE_SIZE = 18;
    private static final int RECIPES_PER_ROW = 9;

    public AssemblingMachineScreen(AssemblingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageHeight = AssemblingMachineMenu.PLAYER_INVENTORY_Y + 82;
        inventoryLabelY = imageHeight - 94;
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

        // 레시피 버튼: 선택된 것은 노랑, 마우스를 올린 것은 밝게
        List<RecipeHolder<AssemblingRecipe>> recipes = menu.getRecipes();
        int hovered = recipeAt(mouseX, mouseY);
        for (int i = 0; i < recipes.size(); i++) {
            int x = recipeButtonX(i);
            int y = recipeButtonY(i);
            int color = i == menu.getSelectedIndex() ? 0xFFE6B428 : i == hovered ? 0xFFAAAAAA : 0xFF8B8B8B;
            graphics.fill(x, y, x + RECIPE_SIZE, y + RECIPE_SIZE, color);
            graphics.renderItem(recipes.get(i).value().result(), x + 1, y + 1);
        }

        // 진행 바: 입력 칸과 출력 칸 사이
        int barX = leftPos + 102;
        int barY = topPos + AssemblingMachineMenu.MACHINE_ROW_Y + 4;
        graphics.fill(barX, barY, barX + 26, barY + 8, 0xFF8B8B8B);
        graphics.fill(barX, barY, barX + menu.getScaledProgress(26), barY + 8, 0xFFE6B428);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int hovered = recipeAt(mouseX, mouseY);
        if (hovered >= 0) {
            graphics.renderTooltip(font, menu.getRecipes().get(hovered).value().result(), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = recipeAt(mouseX, mouseY);
        if (index >= 0 && menu.clickMenuButton(minecraft.player, index)) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            // 바닐라 메뉴 버튼 패킷으로 버튼 번호를 서버에 보냄 → 서버에서 clickMenuButton 실행
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int recipeButtonX(int index) {
        return leftPos + RECIPE_X + (index % RECIPES_PER_ROW) * RECIPE_SIZE;
    }

    private int recipeButtonY(int index) {
        return topPos + RECIPE_Y + (index / RECIPES_PER_ROW) * RECIPE_SIZE;
    }

    // 마우스 위치의 레시피 번호. 없으면 -1
    private int recipeAt(double mouseX, double mouseY) {
        for (int i = 0; i < menu.getRecipes().size(); i++) {
            int x = recipeButtonX(i);
            int y = recipeButtonY(i);
            if (mouseX >= x && mouseX < x + RECIPE_SIZE && mouseY >= y && mouseY < y + RECIPE_SIZE) {
                return i;
            }
        }
        return -1;
    }
}
