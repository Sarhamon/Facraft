package io.github.sarhamon.facraft.machine;

import java.util.List;

import javax.annotation.Nullable;

import io.github.sarhamon.facraft.Facraft;
import io.github.sarhamon.facraft.recipe.AssemblingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AssemblingMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = AssemblingMachineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_SLOTS_END = MACHINE_SLOTS + 36;
    public static final int MACHINE_ROW_Y = 55;
    public static final int PLAYER_INVENTORY_Y = 104;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable
    private final AssemblingMachineBlockEntity machine; // 서버에만 있음
    // 서버와 클라이언트가 같은 순서로 정렬한 목록. 버튼 번호 = 이 목록의 인덱스
    private final List<RecipeHolder<AssemblingRecipe>> recipes;
    private final DataSlot selectedIndex;

    // 클라이언트용: 빈 인벤토리로 만들고, 실제 내용은 서버가 슬롯/데이터 동기화로 채워 줌
    public AssemblingMachineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(MACHINE_SLOTS), new SimpleContainerData(2), null);
    }

    // 서버용: BlockEntity에 직접 연결
    public AssemblingMachineMenu(int containerId, Inventory playerInventory, AssemblingMachineBlockEntity machine) {
        this(containerId, playerInventory, machine.getInventory(), machine.getData(), machine);
    }

    private AssemblingMachineMenu(int containerId, Inventory playerInventory, IItemHandler items, ContainerData data,
                                  @Nullable AssemblingMachineBlockEntity machine) {
        super(Facraft.ASSEMBLING_MACHINE_MENU.get(), containerId);
        this.machine = machine;
        this.access = machine == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(playerInventory.player.level(), machine.getBlockPos());
        this.data = data;
        this.recipes = AssemblingRecipe.sortedRecipes(playerInventory.player.level());
        this.selectedIndex = machine == null ? DataSlot.standalone() : new DataSlot() {
            @Override
            public int get() {
                return indexOf(machine.getSelectedRecipeId());
            }

            @Override
            public void set(int value) {
            }
        };
        addDataSlots(data);
        addDataSlot(selectedIndex);

        for (int i = 0; i < AssemblingMachineBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(items, i, 26 + i * 18, MACHINE_ROW_Y));
        }
        addSlot(new SlotItemHandler(items, AssemblingMachineBlockEntity.OUTPUT_SLOT, 134, MACHINE_ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 출력 칸에는 직접 넣을 수 없음
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INVENTORY_Y + 58));
        }
    }

    private int indexOf(@Nullable ResourceLocation id) {
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public List<RecipeHolder<AssemblingRecipe>> getRecipes() {
        return recipes;
    }

    public int getSelectedIndex() {
        return selectedIndex.get();
    }

    // 레시피 버튼 클릭. 클라이언트에서 먼저 검사하고, 같은 번호가 서버로 가서 다시 호출됨
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= recipes.size()) {
            return false;
        }
        if (machine != null) {
            machine.setSelectedRecipe(recipes.get(id).id());
        }
        return true;
    }

    // 진행 바 길이 계산용. 0 ~ width
    public int getScaledProgress(int width) {
        int progress = data.get(0);
        int total = data.get(1);
        return total == 0 ? 0 : progress * width / total;
    }

    // Shift+클릭: 기계 칸 → 플레이어 인벤토리, 플레이어 인벤토리 → 입력 칸
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        boolean moved = index < MACHINE_SLOTS
                ? moveItemStackTo(stack, MACHINE_SLOTS, PLAYER_SLOTS_END, true)
                : moveItemStackTo(stack, 0, AssemblingMachineBlockEntity.INPUT_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Facraft.ASSEMBLING_MACHINE.get());
    }
}
