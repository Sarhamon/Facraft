package io.github.sarhamon.facraft.recipe;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * 조립기 레시피. 팩토리오처럼 재료 개수와 제작 시간(초, 제작 속도 1 기준)을 가짐.
 * 실제 걸리는 시간 = time / 기계 제작 속도.
 */
public record AssemblingRecipe(List<SizedIngredient> ingredients, ItemStack result, float time) implements Recipe<AssemblingInput> {

    // 재료마다, 입력 칸 전체에서 그 재료에 맞는 아이템 개수를 합쳐 필요한 개수 이상인지 확인
    @Override
    public boolean matches(AssemblingInput input, Level level) {
        for (SizedIngredient ingredient : ingredients) {
            int available = 0;
            for (ItemStack stack : input.items()) {
                if (ingredient.ingredient().test(stack)) {
                    available += stack.getCount();
                }
            }
            if (available < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(AssemblingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    // 바닐라 레시피 북에 안 뜨게 함 (안 하면 클라이언트 로그에 알 수 없는 카테고리 경고가 남음)
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Facraft.ASSEMBLING_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Facraft.ASSEMBLING_RECIPE_TYPE.get();
    }

    // JSON(데이터팩) ↔ 객체, 그리고 서버 → 클라이언트 전송 형식
    public static class Serializer implements RecipeSerializer<AssemblingRecipe> {
        private static final MapCodec<AssemblingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(AssemblingRecipe::ingredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AssemblingRecipe::result),
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("time").forGetter(AssemblingRecipe::time))
                .apply(instance, AssemblingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AssemblingRecipe::ingredients,
                ItemStack.STREAM_CODEC, AssemblingRecipe::result,
                ByteBufCodecs.FLOAT, AssemblingRecipe::time,
                AssemblingRecipe::new);

        @Override
        public MapCodec<AssemblingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
