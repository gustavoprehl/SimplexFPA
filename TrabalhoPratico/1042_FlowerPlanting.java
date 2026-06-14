import java.util.*;

class Solution {
    public int[] gardenNoAdj(int n, int[][] paths) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            graph.add(new ArrayList<>());
        }
        for (int[] path : paths) {
            graph.get(path[0]).add(path[1]);
            graph.get(path[1]).add(path[0]);
        }

        int[] result = new int[n + 1];
        for (int garden = 1; garden <= n; garden++) {
            boolean[] used = new boolean[5];
            for (int neighbor : graph.get(garden)) {
                used[result[neighbor]] = true;
            }
            for (int color = 1; color <= 4; color++) {
                if (!used[color]) {
                    result[garden] = color;
                    break;
                }
            }
        }
        return Arrays.copyOfRange(result, 1, n + 1);
    }
}