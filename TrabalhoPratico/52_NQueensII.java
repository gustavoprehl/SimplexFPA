class Solution {
    private int count = 0;
    private int n;

    public int totalNQueens(int n) {
        this.n = n;
        this.count = 0;
        backtrack(0, 0, 0, 0);
        return count;
    }

    private void backtrack(int row, int cols, int diag1, int diag2) {
        if (row == n) {
            count++;
            return;
        }
        int available = ((1 << n) - 1) & ~(cols | diag1 | diag2);
        while (available != 0) {
            int pos = available & (-available);
            available -= pos;
            backtrack(row + 1, cols | pos, (diag1 | pos) << 1, (diag2 | pos) >> 1);
        }
    }
}
