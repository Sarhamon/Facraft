package io.github.sarhamon.facraft.belt;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.item.ItemStack;

/**
 * 라인의 레인 하나. 팩토리오식 간격 목록.
 *
 * 아이템은 앞(출구)부터 순서대로 저장. gaps[0] = 라인 끝에서 첫 아이템까지 거리,
 * gaps[i] = 아이템 i-1에서 아이템 i까지 거리 (항상 MIN_SPACING 이상).
 * 매 틱 "줄일 수 있는 첫 간격" 하나만 줄이면 그 뒤 아이템이 전부 함께 전진함.
 */
final class BeltLane {
    static final int MIN_SPACING = 64; // 1/4칸 → 레인당 한 칸에 4개

    private final List<ItemStack> items = new ArrayList<>();
    private final IntArrayList gaps = new IntArrayList();
    // 이보다 앞의 간격은 전부 최소값(꽉 참). 아이템이 추가/제거되면 0으로 리셋
    private int firstMovable;

    // 한 틱 전진. 움직였으면 true
    boolean tick(int speed) {
        for (int i = firstMovable; i < gaps.size(); i++) {
            int min = i == 0 ? 0 : MIN_SPACING;
            int gap = gaps.getInt(i);
            if (gap > min) {
                gaps.set(i, gap - Math.min(speed, gap - min));
                firstMovable = i;
                return true;
            }
        }
        firstMovable = gaps.size();
        return false;
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    // 맨 앞 아이템이 라인 끝에 닿았는지
    boolean frontAtEnd() {
        return !items.isEmpty() && gaps.getInt(0) == 0;
    }

    ItemStack front() {
        return items.get(0);
    }

    // 맨 앞 아이템 제거. 다음 아이템의 간격은 "끝에 있던 아이템까지 거리" = "끝까지 거리"라 그대로 유효
    void removeFront() {
        items.remove(0);
        gaps.removeInt(0);
        firstMovable = 0;
    }

    /**
     * 라인 끝에서 distFromEnd 떨어진 곳에 삽입. 앞뒤 아이템과 최소 간격이 안 되면 false.
     */
    boolean insert(int distFromEnd, ItemStack stack) {
        int prev = 0; // 앞 아이템의 끝에서 거리 (없으면 라인 끝 = 0)
        boolean hasPrev = false;
        for (int i = 0; i <= items.size(); i++) {
            int current = i < items.size() ? prev + gaps.getInt(i) : Integer.MAX_VALUE;
            if (distFromEnd <= current) {
                if (hasPrev && distFromEnd - prev < MIN_SPACING) {
                    return false;
                }
                if (i < items.size() && current - distFromEnd < MIN_SPACING) {
                    return false;
                }
                gaps.add(i, distFromEnd - prev);
                if (i < items.size()) {
                    // 원래 i번 아이템(이제 i+1)의 간격은 새 아이템 기준으로 다시 계산
                    gaps.set(i + 1, current - distFromEnd);
                }
                items.add(i, stack);
                firstMovable = 0;
                return true;
            }
            prev = current;
            hasPrev = true;
        }
        return false;
    }

    interface ItemVisitor {
        void visit(int distFromEnd, ItemStack stack);
    }

    void forEach(ItemVisitor visitor) {
        int dist = 0;
        for (int i = 0; i < items.size(); i++) {
            dist += gaps.getInt(i);
            visitor.visit(dist, items.get(i));
        }
    }
}
