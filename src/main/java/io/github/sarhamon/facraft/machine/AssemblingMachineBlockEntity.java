package io.github.sarhamon.facraft.machine;

import java.util.ArrayList;
import java.util.List;

import io.github.sarhamon.facraft.Facraft;
import io.github.sarhamon.facraft.recipe.AssemblingInput;
import io.github.sarhamon.facraft.recipe.AssemblingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.items.ItemStackHandler;

public class AssemblingMachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    public static final int SLOT_COUNT = INPUT_SLOTS + 1;
    // 팩토리오 조립기 1과 같은 제작 속도
    public static final float CRAFTING_SPEED = 0.5f;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged(); // 청크가 저장 대상이라고 표시
        }
    };

    private int progress;
    private int totalTicks;

    // GUI 진행 바용으로 클라이언트에 동기화되는 값: 0 = 진행 틱, 1 = 총 틱
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : totalTicks;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value; else totalTicks = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public AssemblingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(Facraft.ASSEMBLING_MACHINE_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // 서버에서 매 틱 호출 (초당 20회)
    public static void serverTick(Level level, BlockPos pos, BlockState state, AssemblingMachineBlockEntity machine) {
        machine.tick(level);
    }

    private void tick(Level level) {
        // 2-2 임시: 들어 있는 재료로 레시피를 자동으로 찾음. 2-3에서 GUI로 선택한 레시피로 바꿀 예정
        AssemblingRecipe recipe = level.getRecipeManager()
                .getRecipeFor(Facraft.ASSEMBLING_RECIPE_TYPE.get(), currentInput(), level)
                .map(RecipeHolder::value)
                .orElse(null);

        if (recipe == null || !inventory.insertItem(OUTPUT_SLOT, recipe.result().copy(), true).isEmpty()) {
            if (progress != 0 || totalTicks != 0) {
                progress = 0;
                totalTicks = 0;
                setChanged();
            }
            return;
        }

        totalTicks = Math.max(1, Math.round(recipe.time() * 20 / CRAFTING_SPEED));
        progress++;
        if (progress >= totalTicks) {
            consumeIngredients(recipe);
            inventory.insertItem(OUTPUT_SLOT, recipe.result().copy(), false);
            progress = 0;
        }
        setChanged();
    }

    private AssemblingInput currentInput() {
        List<ItemStack> items = new ArrayList<>(INPUT_SLOTS);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            items.add(inventory.getStackInSlot(i));
        }
        return new AssemblingInput(items);
    }

    private void consumeIngredients(AssemblingRecipe recipe) {
        for (SizedIngredient ingredient : recipe.ingredients()) {
            int remaining = ingredient.count();
            for (int i = 0; i < INPUT_SLOTS && remaining > 0; i++) {
                if (ingredient.ingredient().test(inventory.getStackInSlot(i))) {
                    remaining -= inventory.extractItem(i, remaining, false).getCount();
                }
            }
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.facraft.assembling_machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AssemblingMachineMenu(containerId, playerInventory, inventory, data, worldPosition);
    }
}
