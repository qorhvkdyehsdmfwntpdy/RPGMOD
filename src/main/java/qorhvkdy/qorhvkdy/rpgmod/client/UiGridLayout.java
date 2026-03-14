package qorhvkdy.qorhvkdy.rpgmod.client;

/**
 * GUI 버튼 정렬용 경량 그리드 유틸.
 * 화면마다 같은 계산 코드를 반복하지 않기 위해 분리한다.
 */
public final class UiGridLayout {
    private final int originX;
    private final int originY;
    private final int cellWidth;
    private final int cellHeight;
    private final int colGap;
    private final int rowGap;

    public UiGridLayout(int originX, int originY, int cellWidth, int cellHeight, int colGap, int rowGap) {
        this.originX = originX;
        this.originY = originY;
        this.cellWidth = Math.max(1, cellWidth);
        this.cellHeight = Math.max(1, cellHeight);
        this.colGap = Math.max(0, colGap);
        this.rowGap = Math.max(0, rowGap);
    }

    public Rect rect(int col, int row) {
        return rect(col, row, 1, 1);
    }

    public Rect rect(int col, int row, int colSpan, int rowSpan) {
        int safeCol = Math.max(0, col);
        int safeRow = Math.max(0, row);
        int safeColSpan = Math.max(1, colSpan);
        int safeRowSpan = Math.max(1, rowSpan);

        int x = originX + safeCol * (cellWidth + colGap);
        int y = originY + safeRow * (cellHeight + rowGap);
        int w = cellWidth * safeColSpan + colGap * (safeColSpan - 1);
        int h = cellHeight * safeRowSpan + rowGap * (safeRowSpan - 1);
        return new Rect(x, y, w, h);
    }

    public record Rect(int x, int y, int width, int height) {
    }
}
