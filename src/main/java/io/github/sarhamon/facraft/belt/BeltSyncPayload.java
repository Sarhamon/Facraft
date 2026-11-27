package io.github.sarhamon.facraft.belt;

import java.util.List;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 서버 → 클라이언트: 벨트 칸별 아이템 목록. 클라이언트는 받은 그대로 그리기만 함.
 * (3-1 임시 방식. 3-6에서 클라이언트도 같은 시뮬레이션을 돌리고 이벤트만 받도록 바꿀 예정)
 */
public record BeltSyncPayload(List<TileData> tiles) implements CustomPacketPayload {
    public static final Type<BeltSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Facraft.MODID, "belt_sync"));

    public record TileData(BlockPos pos, List<BeltItem> items) {
        static final StreamCodec<RegistryFriendlyByteBuf, TileData> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TileData::pos,
                BeltItem.STREAM_CODEC.apply(ByteBufCodecs.list()), TileData::items,
                TileData::new);
    }

    static final StreamCodec<RegistryFriendlyByteBuf, BeltSyncPayload> STREAM_CODEC =
            TileData.STREAM_CODEC.apply(ByteBufCodecs.list()).map(BeltSyncPayload::new, BeltSyncPayload::tiles);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, BeltSyncPayload::handle);
    }

    // 클라이언트 메인 스레드에서 실행됨
    private static void handle(BeltSyncPayload payload, IPayloadContext context) {
        Level level = context.player().level();
        for (TileData tile : payload.tiles()) {
            if (level.getBlockEntity(tile.pos()) instanceof TransportBeltBlockEntity belt) {
                belt.setItems(tile.items());
            }
        }
    }
}
