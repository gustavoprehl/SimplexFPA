import java.util.*;

class Solution {
    public int longestCycle(int[] edges) {
        int n = edges.length;
        int[] visited = new int[n];
        int[] dist = new int[n];
        int maxLen = -1;
        int time = 1;

        for (int i = 0; i < n; i++) {
            if (visited[i] != 0) continue;

            int curr = i, steps = 0;
            while (curr != -1 && visited[curr] == 0) {
                visited[curr] = time;
                dist[curr] = steps;
                steps++;
                curr = edges[curr];
            }
            if (curr != -1 && visited[curr] == time) {
                maxLen = Math.max(maxLen, steps - dist[curr]);
            }
            time++;
        }
        return maxLen;
    }
}