package io.github.sarhamon.facraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.github.sarhamon.facraft.machine.AssemblingMachineBlock;
import io.github.sarhamon.facraft.machine.AssemblingMachineBlockEntity;
import io.github.sarhamon.facraft.machine.AssemblingMachineMenu;
import io.github.sarhamon.facraft.recipe.AssemblingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblingRecipe>> ASSEMBLING_RECIPE_TYPE = RECIPE_TYPES.register("assembling",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "assembling")));
    public static final DeferredHolder<RecipeSerializer<?>, AssemblingRecipe.Serializer> ASSEMBLING_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("assembling",
            AssemblingRecipe.Serializer::new);

    public static final DeferredBlock<AssemblingMachineBlock> ASSEMBLING_MACHINE = BLOCKS.registerBlock("assembling_machine", AssemblingMachineBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> ASSEMBLING_MACHINE_ITEM = ITEMS.registerSimpleBlockItem("assembling_machine", ASSEMBLING_MACHINE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblingMachineBlockEntity>> ASSEMBLING_MACHINE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("assembling_machine",
            () -> BlockEntityType.Builder.of(AssemblingMachineBlockEntity::new, ASSEMBLING_MACHINE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<AssemblingMachineMenu>> ASSEMBLING_MACHINE_MENU = MENUS.register("assembling_machine",
            () -> new MenuType<>(AssemblingMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));

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
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
