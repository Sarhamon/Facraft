package io.github.sarhamon.facraft.belt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.sarhamon.facraft.Facraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 월드(차원)마다 하나. 로드된 벨트 칸을 모아 라인으로 묶고 매 틱 시뮬레이션함. 서버 전용.
 *
 * 라인은 런타임 캐시. 칸이 추가/제거되면 주변 라인의 아이템을 칸 단위로 내보낸 뒤(export)
 * 라인을 다시 만들고 아이템을 다시 넣음(import). 저장도 같은 방식으로 칸 단위로 내보냄.
 * 언로드된 칸은 라인에 없으므로, 언로드된 쪽으로는 아이템이 넘어가지 않고 경계에서 대기함.
 */
@EventBusSubscriber(modid = Facraft.MODID)
public final class BeltManager {
    // 노랑 벨트 레인 속도 (1/256칸/틱). 2레인 × 0.5개/틱 = 초당 20개
    static final int YELLOW_SPEED = 32;

    private static final Map<ServerLevel, BeltManager> MANAGERS = new WeakHashMap<>();

    static final class Tile {
        final BlockPos pos;
        final Direction facing;
        BeltShape shape;
        BeltLine line;
        int index;
        boolean removed;
        boolean dropOnRemove;
        List<BeltItem> pending = new ArrayList<>(); // 라인에 아직 안 들어간 아이템 (불러온 직후, 재구성 중)

        Tile(BlockPos pos, Direction facing, BeltShape shape) {
            this.pos = pos;
            this.facing = facing;
            this.shape = shape;
        }

        // 아이템이 들어오는 칸 위치 (직선이면 뒤, 코너면 옆)
        BlockPos inputPos() {
            return pos.relative(shape.inputSide(facing));
        }

        // 아이템을 내보내는 칸 위치
        BlockPos outputPos() {
            return pos.relative(facing);
        }
    }

    private final ServerLevel level;
    private final Map<BlockPos, Tile> tiles = new HashMap<>();
    private final Set<BeltLine> lines = new LinkedHashSet<>();
    private final Set<BeltLine> dirtyLines = new LinkedHashSet<>();
    private final Set<Tile> unlinked = new LinkedHashSet<>();

    private BeltManager(ServerLevel level) {
        this.level = level;
    }

    public static BeltManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, BeltManager::new);
    }

    // ---- 칸 등록/해제 (BlockEntity에서 호출) ----

    void addTile(BlockPos pos, Direction facing, BeltShape shape, List<BeltItem> savedItems) {
        Tile tile = new Tile(pos.immutable(), facing, shape);
        tile.pending.addAll(savedItems);
        tiles.put(tile.pos, tile);
        unlinked.add(tile);
        markDirtyAround(tile.pos);
    }

    // dropItems: 부서진 경우 true(아이템을 떨굼), 청크 언로드면 false(이미 저장됨)
    void removeTile(BlockPos pos, boolean dropItems) {
        Tile tile = tiles.remove(pos);
        if (tile == null) {
            return;
        }
        tile.removed = true;
        tile.dropOnRemove = dropItems;
        if (tile.line != null) {
            dirtyLines.add(tile.line); // 이미 tiles에서 빠졌으니 markDirtyAround로는 못 찾음
        } else {
            unlinked.remove(tile);
            if (dropItems) {
                tile.pending.forEach(item -> drop(tile.pos, item.stack()));
            }
        }
        markDirtyAround(pos);
    }

    // 주변 벨트 변화로 직선 ↔ 코너가 바뀜. 연결이 달라지므로 주변 라인을 다시 묶음
    void updateShape(BlockPos pos, BeltShape shape) {
        Tile tile = tiles.get(pos);
        if (tile == null || tile.shape == shape) {
            return;
        }
        tile.shape = shape;
        if (tile.line == null) {
            unlinked.add(tile);
        }
        markDirtyAround(pos);
    }

    // 저장용: 이 칸 위의 아이템
    List<BeltItem> exportTile(BlockPos pos) {
        Tile tile = tiles.get(pos);
        if (tile == null) {
            return List.of();
        }
        if (tile.line == null) {
            return List.copyOf(tile.pending);
        }
        List<BeltItem> result = new ArrayList<>();
        exportLine(tile.line, (t, item) -> {
            if (t == tile) {
                result.add(item);
            }
        });
        return result;
    }

    // 플레이어가 우클릭으로 올리기. 자리가 없으면 false
    boolean insert(BlockPos pos, int lane, int offset, ItemStack stack) {
        rebuild();
        Tile tile = tiles.get(pos);
        if (tile == null || tile.line == null) {
            return false;
        }
        BeltLine line = tile.line;
        if (!line.lanes[lane].insert(line.distFromEnd(tile.index, offset), stack)) {
            return false;
        }
        line.changed = true;
        return true;
    }

    // ---- 틱 ----

    private void tick() {
        rebuild();
        for (BeltLine line : lines) {
            if (line.tick(YELLOW_SPEED)) {
                line.changed = true;
            }
        }
        for (BeltLine line : lines) {
            handOff(line);
        }
        sync();
    }

    // 라인 끝에 닿은 아이템을 다음 칸(다른 라인의 시작, 또는 고리 모양이면 자기 시작)으로 넘김
    private void handOff(BeltLine line) {
        Tile next = next(line.last());
        if (next == null || next.line == null) {
            return; // 다음 칸이 없거나, 언로드됐거나, 이 칸에서 받는 방향이 아님 → 대기
        }
        for (int lane = 0; lane < 2; lane++) {
            BeltLane from = line.lanes[lane];
            if (from.frontAtEnd() && next.line.lanes[lane].insert(next.line.distFromEnd(next.index, 0), from.front())) {
                from.removeFront();
                line.changed = true;
                next.line.changed = true;
            }
        }
    }

    // ---- 라인 재구성 ----

    private void markDirtyAround(BlockPos pos) {
        markDirty(pos);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            markDirty(pos.relative(dir));
        }
    }

    private void markDirty(BlockPos pos) {
        Tile tile = tiles.get(pos);
        if (tile != null && tile.line != null) {
            dirtyLines.add(tile.line);
        }
    }

    private void rebuild() {
        if (dirtyLines.isEmpty() && unlinked.isEmpty()) {
            return;
        }
        // 1. 영향받은 라인을 해체하고 아이템을 칸 단위로 내보냄
        for (BeltLine line : dirtyLines) {
            exportLine(line, (tile, item) -> {
                if (!tile.removed) {
                    tile.pending.add(item);
                } else if (tile.dropOnRemove) {
                    drop(tile.pos, item.stack());
                }
            });
            lines.remove(line);
            for (Tile tile : line.tiles) {
                tile.line = null;
                if (!tile.removed) {
                    unlinked.add(tile);
                }
            }
        }
        dirtyLines.clear();

        // 2. 묶이지 않은 칸들로 라인을 다시 만듦
        Set<Tile> todo = new LinkedHashSet<>(unlinked);
        unlinked.clear();
        while (!todo.isEmpty()) {
            Tile start = todo.iterator().next();
            Tile head = start;
            while (true) {
                Tile prev = feeder(head);
                if (prev == null || prev == start || !todo.contains(prev)) {
                    break;
                }
                head = prev;
            }
            List<Tile> chain = new ArrayList<>();
            for (Tile t = head; t != null && todo.remove(t); t = next(t)) {
                chain.add(t);
            }
            BeltLine line = new BeltLine(chain);
            for (int i = 0; i < chain.size(); i++) {
                Tile tile = chain.get(i);
                tile.line = line;
                tile.index = i;
            }
            // 3. 아이템을 다시 넣음
            for (Tile tile : chain) {
                for (BeltItem item : tile.pending) {
                    if (!line.lanes[item.lane()].insert(line.distFromEnd(tile.index, item.offset()), item.stack())) {
                        drop(tile.pos, item.stack()); // 정상이라면 일어나지 않음
                    }
                }
                tile.pending = new ArrayList<>();
            }
            lines.add(line);
        }
    }

    // 이 칸으로 아이템을 넣어 주는 칸: 입력 위치에 있고, 그 칸의 출구가 이 칸이어야 함
    private Tile feeder(Tile tile) {
        Tile prev = tiles.get(tile.inputPos());
        return prev != null && prev.outputPos().equals(tile.pos) ? prev : null;
    }

    // 이 칸이 아이템을 넘겨주는 칸: 출구 위치에 있고, 그 칸의 입력 위치가 이 칸이어야 함
    // (옆으로 들이받는 경우는 사이드 로딩이라 여기 해당 안 됨, 3-3)
    private Tile next(Tile tile) {
        Tile next = tiles.get(tile.outputPos());
        return next != null && next.inputPos().equals(tile.pos) ? next : null;
    }

    interface TileItemVisitor {
        void visit(Tile tile, BeltItem item);
    }

    // 라인 위치(끝에서 거리) → 칸 index와 칸 안 offset으로 바꿔서 전달
    private static void exportLine(BeltLine line, TileItemVisitor visitor) {
        int length = line.length();
        for (int lane = 0; lane < 2; lane++) {
            int laneIndex = lane;
            line.lanes[lane].forEach((distFromEnd, stack) -> {
                int fromStart = length - distFromEnd;
                int index = Math.min(fromStart / BeltLine.TILE_LENGTH, line.tiles.size() - 1);
                int offset = fromStart - index * BeltLine.TILE_LENGTH;
                visitor.visit(line.tiles.get(index), new BeltItem(laneIndex, offset, stack));
            });
        }
    }

    private void drop(BlockPos pos, ItemStack stack) {
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack);
    }

    // ---- 동기화 (3-1 임시: 바뀐 라인의 칸별 상태를 매 틱 전송. 3-6에서 결정론적 방식으로 교체) ----

    private void sync() {
        Map<ChunkPos, List<BeltSyncPayload.TileData>> byChunk = new HashMap<>();
        for (BeltLine line : lines) {
            if (!line.changed) {
                continue;
            }
            line.changed = false;
            Map<Tile, List<BeltItem>> perTile = new HashMap<>();
            for (Tile tile : line.tiles) {
                perTile.put(tile, new ArrayList<>());
            }
            exportLine(line, (tile, item) -> perTile.get(tile).add(item));
            perTile.forEach((tile, items) -> byChunk.computeIfAbsent(new ChunkPos(tile.pos), c -> new ArrayList<>())
                    .add(new BeltSyncPayload.TileData(tile.pos, items)));
        }
        byChunk.forEach((chunk, data) -> PacketDistributor.sendToPlayersTrackingChunk(level, chunk, new BeltSyncPayload(data)));
    }

    // ---- 이벤트 ----

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BeltManager manager = MANAGERS.get(serverLevel);
            if (manager != null) {
                manager.tick();
            }
        }
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            MANAGERS.remove(serverLevel);
        }
    }
}
