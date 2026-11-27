package io.github.sarhamon.facraft.belt;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 벨트 칸 하나 위의 아이템 하나. 저장, 패킷, 렌더링에 쓰는 형태.
 * lane: 0 = 진행 방향 왼쪽, 1 = 오른쪽. offset: 칸 안 위치 0(뒤 끝) ~ 256(앞 끝).
 */
public record BeltItem(int lane, int offset, ItemStack stack) {
    public static final StreamCodec<RegistryFriendlyByteBuf, BeltItem> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BeltItem::lane,
            ByteBufCodecs.VAR_INT, BeltItem::offset,
            ItemStack.STREAM_CODEC, BeltItem::stack,
            BeltItem::new);

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("lane", lane);
        tag.putInt("offset", offset);
        tag.put("item", stack.save(registries));
        return tag;
    }

    public static BeltItem load(CompoundTag tag, HolderLookup.Provider registries) {
        return new BeltItem(tag.getInt("lane"), tag.getInt("offset"), ItemStack.parseOptional(registries, tag.getCompound("item")));
    }
}
