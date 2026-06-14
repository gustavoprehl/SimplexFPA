import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        StreamTokenizer in = new StreamTokenizer(new BufferedReader(new InputStreamReader(System.in)));
        StringBuilder sb = new StringBuilder();

        in.nextToken();
        int t = (int) in.nval;

        while (t-- > 0) {
            in.nextToken();
            int n = (int) in.nval;
            in.nextToken();
            int m = (int) in.nval;

            int[] a = new int[n * m];
            for (int i = 0; i < n * m; i++) {
                in.nextToken();
                a[i] = (int) in.nval;
            }

            boolean[] visited = new boolean[n * m];
            int[] queue = new int[n * m];
            long best = 0;

            for (int start = 0; start < n * m; start++) {
                if (a[start] == 0 || visited[start]) continue;

                int head = 0, tail = 0;
                queue[tail++] = start;
                visited[start] = true;
                long volume = 0;

                while (head < tail) {
                    int cell = queue[head++];
                    volume += a[cell];
                    int r = cell / m, c = cell % m;

                    if (r > 0) {
                        int nb = cell - m;
                        if (!visited[nb] && a[nb] > 0) {
                            visited[nb] = true;
                            queue[tail++] = nb;
                        }
                    }
                    if (r < n - 1) {
                        int nb = cell + m;
                        if (!visited[nb] && a[nb] > 0) {
                            visited[nb] = true;
                            queue[tail++] = nb;
                        }
                    }
                    if (c > 0) {
                        int nb = cell - 1;
                        if (!visited[nb] && a[nb] > 0) {
                            visited[nb] = true;
                            queue[tail++] = nb;
                        }
                    }
                    if (c < m - 1) {
                        int nb = cell + 1;
                        if (!visited[nb] && a[nb] > 0) {
                            visited[nb] = true;
                            queue[tail++] = nb;
                        }
                    }
                }

                best = Math.max(best, volume);
            }

            sb.append(best).append('\n');
        }

        System.out.print(sb);
    }
}