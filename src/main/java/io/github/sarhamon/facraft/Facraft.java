package io.github.sarhamon.facraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Facraft.MODID)
public class Facraft {
    public static final String MODID = "facraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 로드맵 2단계에서 BlockEntity와 GUI를 붙일 조립기. 지금은 단순 블록.
    public static final DeferredBlock<Block> ASSEMBLING_MACHINE = BLOCKS.registerSimpleBlock("assembling_machine",
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> ASSEMBLING_MACHINE_ITEM = ITEMS.registerSimpleBlockItem("assembling_machine", ASSEMBLING_MACHINE);

    public static final DeferredItem<Item> IRON_GEAR_WHEEL = ITEMS.registerSimpleItem("iron_gear_wheel");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FACRAFT_TAB = CREATIVE_MODE_TABS.register("facraft", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.facraft"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> IRON_GEAR_WHEEL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(IRON_GEAR_WHEEL.get());
                output.accept(ASSEMBLING_MACHINE_ITEM.get());
            }).build());

    public Facraft(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
