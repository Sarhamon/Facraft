package io.github.sarhamon.facraft.belt;

import java.util.ArrayList;
import java.util.List;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 벨트 칸. 틱을 돌지 않음 (시뮬레이션은 BeltManager가 라인 단위로 함).
 * 서버: 저장/불러오기 창구. 클라이언트: 렌더링용 아이템 목록을 들고 있음.
 */
public class TransportBeltBlockEntity extends BlockEntity {
    // 서버: 불러온 직후 매니저에 넘기기 전까지 임시 보관. 클라이언트: 렌더링할 아이템
    private List<BeltItem> items = List.of();
    private boolean unloading;

    public TransportBeltBlockEntity(BlockPos pos, BlockState state) {
        super(Facraft.TRANSPORT_BELT_BLOCK_ENTITY.get(), pos, state);
    }

    public List<BeltItem> getItems() {
        return items;
    }

    void setItems(List<BeltItem> items) {
        this.items = items;
    }

    // 월드에 추가될 때(설치, 청크 로드) 매니저에 등록
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            BeltManager.get(serverLevel).addTile(worldPosition, state.getValue(TransportBeltBlock.FACING),
                    state.getValue(TransportBeltBlock.SHAPE), items);
            items = List.of();
        }
    }

    // 청크 언로드: 아이템은 이미 저장됐으니 떨구지 않고 매니저에서만 뺌
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloading = true;
        if (level instanceof ServerLevel serverLevel) {
            BeltManager.get(serverLevel).removeTile(worldPosition, false);
        }
    }

    // 블록이 부서짐: 칸 위 아이템을 떨굼 (언로드 때도 호출되므로 구분)
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!unloading && level instanceof ServerLevel serverLevel) {
            BeltManager.get(serverLevel).removeTile(worldPosition, true);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        List<BeltItem> current = level instanceof ServerLevel serverLevel ? BeltManager.get(serverLevel).exportTile(worldPosition) : items;
        ListTag list = new ListTag();
        for (BeltItem item : current) {
            list.add(item.save(registries));
        }
        tag.put("items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        List<BeltItem> loaded = new ArrayList<>();
        for (Tag entry : tag.getList("items", Tag.TAG_COMPOUND)) {
            loaded.add(BeltItem.load((CompoundTag) entry, registries));
        }
        items = loaded;
    }

    // 플레이어가 청크를 처음 받을 때 함께 보내는 데이터 (현재 아이템 포함)
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
