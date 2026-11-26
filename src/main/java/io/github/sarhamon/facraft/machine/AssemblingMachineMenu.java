package io.github.sarhamon.facraft.machine;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AssemblingMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = AssemblingMachineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_SLOTS_END = MACHINE_SLOTS + 36;

    private final ContainerLevelAccess access;

    // 클라이언트용: 빈 인벤토리로 만들고, 실제 내용은 서버가 슬롯 동기화로 채워 줌
    public AssemblingMachineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(MACHINE_SLOTS), null);
    }

    // 서버용: BlockEntity의 실제 인벤토리에 연결
    public AssemblingMachineMenu(int containerId, Inventory playerInventory, IItemHandler machine, BlockPos pos) {
        super(Facraft.ASSEMBLING_MACHINE_MENU.get(), containerId);
        this.access = pos == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(playerInventory.player.level(), pos);

        for (int i = 0; i < AssemblingMachineBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(machine, i, 26 + i * 18, 35));
        }
        addSlot(new SlotItemHandler(machine, AssemblingMachineBlockEntity.OUTPUT_SLOT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // 출력 칸에는 직접 넣을 수 없음
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
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
