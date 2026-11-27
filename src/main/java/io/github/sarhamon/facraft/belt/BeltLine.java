package io.github.sarhamon.facraft.belt;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * 이어진 벨트 칸들의 묶음. 레인 2개를 가짐. 런타임 캐시라 저장되지 않음
 * (저장은 칸 단위로 내보냄, BeltManager 참고).
 */
final class BeltLine {
    static final int TILE_LENGTH = 256;

    final List<BeltManager.Tile> tiles; // 뒤 → 앞(출구) 순서
    final BeltLane[] lanes = {new BeltLane(), new BeltLane()};
    boolean changed = true; // 이번 틱에 클라이언트로 보낼 변화가 있는지

    BeltLine(List<BeltManager.Tile> tiles) {
        this.tiles = tiles;
    }

    int length() {
        return tiles.size() * TILE_LENGTH;
    }

    BeltManager.Tile last() {
        return tiles.get(tiles.size() - 1);
    }

    // 라인 끝 다음 칸 위치 (아이템이 넘어갈 곳)
    BlockPos nextPos() {
        BeltManager.Tile last = last();
        return last.pos.relative(last.facing);
    }

    // 칸 index, 칸 안 offset → 라인 끝에서 거리
    int distFromEnd(int tileIndex, int offset) {
        return length() - (tileIndex * TILE_LENGTH + offset);
    }

    boolean tick(int speed) {
        boolean moved = lanes[0].tick(speed);
        moved |= lanes[1].tick(speed);
        return moved;
    }
}
