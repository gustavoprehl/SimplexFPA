import java.util.*;

/**
 * =============================================================================
 * SIMPLEX ALGORITHM — Implementação baseada no livro CLRS09
 * (Cormen, Leiserson, Rivest, Stein — "Introduction to Algorithms", 3ª ed.,
 *  Capítulo 29: Linear Programming)
 *
 * Resolve Programas Lineares na FORMA PADRÃO:
 *   maximizar   c^T x
 *   sujeito a   A x <= b
 *               x >= 0
 *
 * Internamente usa a FORMA SLACK (forma canônica do Simplex):
 *   Para cada variável básica i em B:
 *       x_i = b_i - sum_{j in N} a_{ij} * x_j
 *   Função objetivo:
 *       z  = v   + sum_{j in N} c_j   * x_j
 *
 * Notação:
 *   n  = número de variáveis de decisão originais
 *   m  = número de restrições
 *   N  = conjunto de índices das variáveis NÃO-BÁSICAS  (tamanho n)
 *   B  = conjunto de índices das variáveis BÁSICAS       (tamanho m)
 *   A  = matriz de coeficientes na forma slack           (m x n)
 *   b  = vetor de termos independentes (valores das vars básicas quando N=0)
 *   c  = vetor de coeficientes da função objetivo (vars não-básicas)
 *   v  = valor atual da função objetivo (quando todas vars não-básicas = 0)
 *
 * Variáveis são numeradas de 1 a n+m:
 *   1..n      → variáveis de decisão originais (inicialmente não-básicas)
 *   n+1..n+m  → variáveis de folga (slack) introduzidas para as restrições
 *   n+m+1     → variável artificial x_0 (usada apenas na inicialização)
 * =============================================================================
 */
public class Simplex {

    // -------------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------------

    /** Tolerância para comparações de ponto flutuante */
    private static final double EPS = 1e-9;

    // -------------------------------------------------------------------------
    // Estado interno da forma slack
    // -------------------------------------------------------------------------

    private int origN; // n original (vars de decisão)
    private int origM; // m original (restrições)

    /** Índices das variáveis não-básicas (array de tamanho variável) */
    private int[] N;

    /** Índices das variáveis básicas (array de tamanho origM) */
    private int[] B;

    /**
     * Matriz de coeficientes na forma slack.
     * A[i][j] = coeficiente da variável não-básica N[j] na equação da variável
     * básica B[i].
     */
    private double[][] A;

    /**
     * Vetor de termos independentes.
     * b[i] = valor de B[i] quando todos N[j] = 0 (solução básica atual).
     */
    private double[] b;

    /**
     * Coeficientes da função objetivo para variáveis não-básicas.
     * c[j] = coeficiente de N[j] na função objetivo.
     */
    private double[] c;

    /** Valor atual da função objetivo (com todas vars não-básicas = 0) */
    private double v;

    // -------------------------------------------------------------------------
    // Classe de resultado
    // -------------------------------------------------------------------------

    public static class Result {
        public enum Status { OPTIMAL, INFEASIBLE, UNBOUNDED }

        public final Status status;
        public final double optimalValue;
        public final double[] solution; // x[0..n-1] = valores de x_1..x_n

        public Result(Status s, double val, double[] sol) {
            this.status = s;
            this.optimalValue = val;
            this.solution = sol;
        }
    }

    // -------------------------------------------------------------------------
    // Utilitários internos
    // -------------------------------------------------------------------------

    /** Retorna a posição de 'val' no array 'arr', ou -1 se não encontrado */
    private int pos(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i] == val) return i;
        return -1;
    }

    /** Cópia profunda de matriz double[][] */
    private double[][] deepCopy(double[][] mat) {
        double[][] copy = new double[mat.length][];
        for (int i = 0; i < mat.length; i++)
            copy[i] = mat[i].clone();
        return copy;
    }

    // =========================================================================
    // PROCEDIMENTO PIVOT  (CLRS, p. 869)
    // =========================================================================
    /**
     * PIVOT(N, B, A, b, c, v, l, e)
     *
     * Realiza uma operação de pivô: a variável 'e' (não-básica) ENTRA na base
     * e a variável 'l' (básica) SAI da base.
     *
     * Complexidade: O(n * m)  — um pivô atualiza todos os m coeficientes de
     * cada uma das n colunas, além do vetor b e do vetor c.
     *
     * @param l valor (índice global) da variável que SAI da base (estava em B)
     * @param e valor (índice global) da variável que ENTRA na base (estava em N)
     */
    private void pivot(int l, int e) {
        int n = N.length;
        int m = B.length;

        int ePos = pos(N, e); // coluna de e em N
        int lPos = pos(B, l); // linha de l em B

        // Aloca novos arrays para a forma slack resultante
        double[][] newA = new double[m][n];
        double[] newb   = new double[m];
        double[] newc   = new double[n];
        // newv será atualizado direto em 'v'

        // -----------------------------------------------------------------
        // Passo 1 — CLRS linhas 3–6:
        // Calcula nova equação para x_e (a nova variável básica no lugar de l).
        // Isola x_e na equação da linha l:
        //   x_l = b_l/a_{le} - sum_{j≠e} (a_{lj}/a_{le})*x_j - (1/a_{le})*x_e
        // Reinterpretando: x_e = b_l/a_{le} - sum_{j≠e}(a_{lj}/a_{le})*x_j + (1/a_{le})*x_l
        // -----------------------------------------------------------------
        newb[lPos] = b[lPos] / A[lPos][ePos];
        for (int j = 0; j < n; j++) {
            if (j != ePos)
                newA[lPos][j] = A[lPos][j] / A[lPos][ePos];
        }
        // Coeficiente de x_l (agora não-básica) na linha de x_e
        newA[lPos][ePos] = 1.0 / A[lPos][ePos];

        // -----------------------------------------------------------------
        // Passo 2 — CLRS linhas 8–12:
        // Atualiza as demais equações, substituindo x_e pela sua nova expressão.
        // Para cada i ≠ l em B:
        //   x_i = b_i - a_{ie}*x_e - ...
        //       = b_i - a_{ie}*(b_l/a_{le} - sum_{j≠e}(a_{lj}/a_{le})*x_j +(1/a_{le})*x_l) - ...
        // -----------------------------------------------------------------
        for (int i = 0; i < m; i++) {
            if (i == lPos) continue;
            newb[i] = b[i] - A[i][ePos] * newb[lPos];
            for (int j = 0; j < n; j++) {
                if (j != ePos)
                    newA[i][j] = A[i][j] - A[i][ePos] * newA[lPos][j];
            }
            newA[i][ePos] = -A[i][ePos] * newA[lPos][ePos];
        }

        // -----------------------------------------------------------------
        // Passo 3 — CLRS linhas 14–17:
        // Atualiza a função objetivo, substituindo x_e.
        // z = v + c_e*b_e + sum_{j≠e} (c_j - c_e*a_{ej})*x_j - c_e*(1/a_{le})*x_l
        // -----------------------------------------------------------------
        v = v + c[ePos] * newb[lPos];
        for (int j = 0; j < n; j++) {
            if (j != ePos)
                newc[j] = c[j] - c[ePos] * newA[lPos][j];
        }
        newc[ePos] = -c[ePos] * newA[lPos][ePos];

        // -----------------------------------------------------------------
        // Passo 4 — CLRS linhas 19–20:
        // Atualiza os conjuntos N e B: e sai de N para B, l sai de B para N.
        // -----------------------------------------------------------------
        N[ePos] = l;
        B[lPos] = e;

        A = newA;
        b = newb;
        c = newc;
    }

    // =========================================================================
    // PROCEDIMENTO SIMPLEX PRINCIPAL  (CLRS, p. 871)
    // =========================================================================
    /**
     * SIMPLEX(A, b, c)  — Algoritmo Simplex conforme CLRS.
     *
     * Recebe o PL na forma padrão e retorna a solução ótima (se existir).
     *
     * COMPLEXIDADE:
     *   - Cada iteração do while executa um PIVOT em O(n*m).
     *   - O número de iterações é, no pior caso, exponencial em n (2^n),
     *     porém, na prática, é polinomial (tipicamente O(n+m)).
     *   - Com a Regra de Bland (menor índice) garante-se terminação finita,
     *     evitando ciclagem (stalling).
     *   - Complexidade por iteração: O(n * m).
     *   - Total no pior caso: O(2^n * n * m).
     *
     * @param origA matriz de restrições m×n (coeficientes de A em Ax <= b)
     * @param origb vetor de lado direito m-dimensional
     * @param origc vetor de coeficientes da função objetivo n-dimensional
     * @return Result com status OPTIMAL, INFEASIBLE ou UNBOUNDED
     */
    public Result solve(double[][] origA, double[] origb, double[] origc) {
        origN = origc.length;
        origM = origb.length;

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         ALGORITMO SIMPLEX — CLRS Cap. 29            ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        printLP(origA, origb, origc);

        // -----------------------------------------------------------------
        // CLRS linha 1: Inicializa a forma slack (ou detecta infeasibilidade)
        // -----------------------------------------------------------------
        boolean feasible = initializeSimplex(origA, origb, origc);
        if (!feasible) {
            System.out.println("\n[RESULTADO] PL INFEASÍVEL — não existe solução viável.");
            return new Result(Result.Status.INFEASIBLE, 0, null);
        }

        System.out.println("\n--- Forma slack inicial ---");
        printSlackForm();

        int iteration = 0;

        // -----------------------------------------------------------------
        // CLRS linhas 3–12: Loop principal — repete enquanto há c_j > 0
        // -----------------------------------------------------------------
        while (true) {
            iteration++;

            // CLRS linha 4: Escolha da variável ENTRANTE 'e'
            // Regra de Bland: escolha o menor índice j em N com c_j > 0
            // (garante terminação — sem ciclagem)
            int e = -1;
            int smallestE = Integer.MAX_VALUE;
            for (int j = 0; j < N.length; j++) {
                if (c[j] > EPS && N[j] < smallestE) {
                    smallestE = N[j];
                    e = N[j];
                }
            }

            // CLRS linha 3: Se não há c_j > 0, a solução atual é ÓTIMA
            if (e == -1) break;

            int ePos = pos(N, e);
            System.out.printf("%n--- Iteração %d ---  variável ENTRANTE: x_%d%n", iteration, e);

            // CLRS linhas 5–8: Calcula Δ_i = b_i / a_{ie} para cada i em B com a_{ie} > 0
            double[] delta = new double[B.length];
            int l = -1;
            double minDelta = Double.POSITIVE_INFINITY;
            int smallestL = Integer.MAX_VALUE;

            System.out.printf("  Razões mínimas (teste de razão):%n");
            for (int i = 0; i < B.length; i++) {
                if (A[i][ePos] > EPS) {
                    delta[i] = b[i] / A[i][ePos];
                    System.out.printf("    x_%d: Δ = %.4f / %.4f = %.4f%n",
                            B[i], b[i], A[i][ePos], delta[i]);
                    // Regra de Bland para desempate: menor índice entre empates
                    if (delta[i] < minDelta - EPS ||
                            (Math.abs(delta[i] - minDelta) < EPS && B[i] < smallestL)) {
                        minDelta = delta[i];
                        l = B[i];
                        smallestL = B[i];
                    }
                } else {
                    delta[i] = Double.POSITIVE_INFINITY;
                    System.out.printf("    x_%d: Δ = ∞ (coeficiente não-positivo)%n", B[i]);
                }
            }

            // CLRS linhas 10–11: Se Δ_l = ∞ para todo i, o PL é ILIMITADO
            if (l == -1) {
                System.out.println("\n[RESULTADO] PL ILIMITADO — função objetivo cresce indefinidamente.");
                return new Result(Result.Status.UNBOUNDED, 0, null);
            }

            System.out.printf("  variável SAINTE: x_%d  (Δ mínimo = %.4f)%n", l, minDelta);

            // CLRS linha 12: Executa o pivô
            pivot(l, e);

            System.out.printf("  Valor objetivo atual: z = %.4f%n", v);
            printSlackForm();
        }

        // -----------------------------------------------------------------
        // CLRS linhas 13–17: Extrai solução ótima
        // x_i = b[pos(B,i)] se i está na base, senão 0
        // -----------------------------------------------------------------
        double[] x = new double[origN];
        for (int i = 1; i <= origN; i++) {
            int bPos = pos(B, i);
            x[i - 1] = (bPos != -1) ? b[bPos] : 0.0;
        }

        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║         SOLUÇÃO ÓTIMA            ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.printf("  Valor ótimo: z* = %.6f%n", v);
        for (int i = 0; i < origN; i++)
            System.out.printf("  x_%d = %.6f%n", i + 1, x[i]);

        return new Result(Result.Status.OPTIMAL, v, x);
    }

    // =========================================================================
    // INICIALIZAÇÃO  (CLRS, p. 886 — INITIALIZE-SIMPLEX)
    // =========================================================================
    /**
     * INITIALIZE-SIMPLEX(A, b, c)
     *
     * Converte o PL para a forma slack e verifica/estabelece viabilidade.
     *
     * Caso 1 — Se min(b_i) >= 0:
     *   A solução básica x = 0 já é viável. Forma slack padrão.
     *
     * Caso 2 — Se algum b_i < 0:
     *   Resolve um PL AUXILIAR (Laux) para encontrar uma BFS viável:
     *   • Adiciona variável artificial x_0 >= 0 a todas as restrições.
     *   • Laux: maximizar -x_0  s.t.  Ax - x_0 <= b,  x, x_0 >= 0
     *   • Se ótimo de Laux = 0  => original é viável  (x_0 = 0 na solução)
     *   • Se ótimo de Laux < 0  => original é infeasível
     *
     * @return true se o PL é viável, false se infeasível
     */
    private boolean initializeSimplex(double[][] origA, double[] origb, double[] origc) {
        int n = origN;
        int m = origM;

        // Encontra o índice da restrição mais violada (menor b_i)
        int kMin = 0;
        for (int i = 1; i < m; i++)
            if (origb[i] < origb[kMin]) kMin = i;

        // -----------------------------------------------------------------
        // CASO 1: Solução básica inicial já é viável
        // -----------------------------------------------------------------
        if (origb[kMin] >= -EPS) {
            System.out.println("\n[INIT] Solução básica inicial é viável (todos b_i >= 0).");
            N = new int[n];
            B = new int[m];
            A = new double[m][n];
            b = new double[m];
            c = new double[n];
            v = 0;

            for (int j = 0; j < n; j++)  N[j] = j + 1;       // vars 1..n
            for (int i = 0; i < m; i++)  B[i] = n + i + 1;   // vars n+1..n+m

            for (int i = 0; i < m; i++) {
                b[i] = origb[i];
                for (int j = 0; j < n; j++) A[i][j] = origA[i][j];
            }
            for (int j = 0; j < n; j++) c[j] = origc[j];
            return true;
        }

        // -----------------------------------------------------------------
        // CASO 2: Precisa do PL auxiliar
        // -----------------------------------------------------------------
        System.out.printf("%n[INIT] b[%d] = %.4f < 0 → resolvendo PL AUXILIAR Laux...%n",
                kMin, origb[kMin]);

        int x0 = n + m + 1; // índice global da variável artificial x_0

        // Monta a forma slack do Laux:
        //   N_aux = {1..n, x_0}   (n+1 não-básicas)
        //   B_aux = {n+1..n+m}    (m básicas, as folgas)
        //   Para cada i: x_{n+i} = b_i - sum_{j=1}^{n} a_{ij}*x_j + x_0
        //   Objetivo: maximizar -x_0  (c_{x_0} = -1, demais = 0)
        N = new int[n + 1];
        B = new int[m];
        A = new double[m][n + 1];
        b = new double[m];
        c = new double[n + 1];
        v = 0;

        for (int j = 0; j < n; j++) N[j] = j + 1;
        N[n] = x0;
        for (int i = 0; i < m; i++) B[i] = n + i + 1;

        for (int i = 0; i < m; i++) {
            b[i] = origb[i];
            for (int j = 0; j < n; j++) A[i][j] = origA[i][j];
            A[i][n] = -1.0; // coef. de x_0 (sinal negativo para Ax - x_0 <= b)
        }
        c[n] = -1.0; // maximizar -x_0

        // Pivô inicial: x_0 entra, B[kMin] sai (torna solução viável para Laux)
        pivot(B[kMin], x0);

        // ---- Loop Simplex para Laux ----
        while (true) {
            int e = -1;
            int smallestE = Integer.MAX_VALUE;
            for (int j = 0; j < N.length; j++) {
                if (c[j] > EPS && N[j] < smallestE) {
                    smallestE = N[j];
                    e = N[j];
                }
            }
            if (e == -1) break;

            int ePos = pos(N, e);
            int l = -1;
            double minDelta = Double.POSITIVE_INFINITY;
            int smallestL = Integer.MAX_VALUE;

            for (int i = 0; i < B.length; i++) {
                if (A[i][ePos] > EPS) {
                    double d = b[i] / A[i][ePos];
                    if (d < minDelta - EPS ||
                            (Math.abs(d - minDelta) < EPS && B[i] < smallestL)) {
                        minDelta = d;
                        l = B[i];
                        smallestL = B[i];
                    }
                }
            }
            if (l == -1) break; // ilimitado (não deve ocorrer no Laux)
            pivot(l, e);
        }

        // Verifica viabilidade: ótimo de Laux deve ser 0 (x_0 = 0)
        if (v < -EPS) {
            System.out.printf("[INIT] Ótimo de Laux = %.6f < 0 → PL original INFEASÍVEL.%n", v);
            return false;
        }

        System.out.printf("[INIT] Ótimo de Laux = %.6f ≈ 0 → PL original é VIÁVEL.%n", v);

        // Se x_0 ainda está na base (com valor 0), pivota-a para fora
        int x0Pos = pos(B, x0);
        if (x0Pos != -1) {
            // Busca qualquer var não-básica j com A[x0Pos][j] ≠ 0 para pivot degenerado
            for (int j = 0; j < N.length; j++) {
                if (N[j] != x0 && Math.abs(A[x0Pos][j]) > EPS) {
                    pivot(B[x0Pos], N[j]);
                    break;
                }
            }
        }

        // Remove x_0 do conjunto N (se ainda lá estiver) e reconstrói arrays sem ela
        int x0NPos = pos(N, x0);
        int newNSize = (x0NPos != -1) ? N.length - 1 : N.length;
        int[] newN   = new int[newNSize];
        double[][] newA = new double[m][newNSize];
        double[] newC   = new double[newNSize];

        int idx = 0;
        for (int j = 0; j < N.length; j++) {
            if (N[j] == x0) continue;
            newN[idx] = N[j];
            for (int i = 0; i < m; i++) newA[i][idx] = A[i][j];
            idx++;
        }

        // Restaura objetivo original, expresso em termos das vars não-básicas atuais.
        // Começa com z = sum_{j=1}^{n} origc[j-1] * x_j.
        // Para cada x_j básica, substitui pela sua expressão na forma slack.
        v = 0;
        Arrays.fill(newC, 0.0);

        for (int j = 1; j <= n; j++) {
            double cj = origc[j - 1];
            if (Math.abs(cj) < EPS) continue;

            int bPos = pos(B, j);
            if (bPos != -1) {
                // x_j é básica: z += cj * (b[bPos] - sum_k newA[bPos][k]*x_{newN[k]})
                v += cj * b[bPos];
                for (int k = 0; k < newNSize; k++)
                    newC[k] -= cj * newA[bPos][k];
            } else {
                // x_j é não-básica: adiciona cj diretamente
                int nPos = pos(newN, j);
                if (nPos != -1) newC[nPos] += cj;
            }
        }

        N = newN;
        A = newA;
        c = newC;
        return true;
    }

    // =========================================================================
    // Utilitário: imprime a forma slack atual
    // =========================================================================
    private void printSlackForm() {
        System.out.printf("  z = %.4f", v);
        for (int j = 0; j < N.length; j++) {
            if (Math.abs(c[j]) > EPS)
                System.out.printf(" %+.4f·x_%d", c[j], N[j]);
        }
        System.out.println();
        for (int i = 0; i < B.length; i++) {
            System.out.printf("  x_%d = %.4f", B[i], b[i]);
            for (int j = 0; j < N.length; j++) {
                if (Math.abs(A[i][j]) > EPS)
                    System.out.printf(" %+.4f·x_%d", -A[i][j], N[j]);
            }
            System.out.println();
        }
    }

    // =========================================================================
    // Utilitário: imprime o PL na forma padrão
    // =========================================================================
    private void printLP(double[][] origA, double[] origb, double[] origc) {
        System.out.println("\nPL na forma padrão:");
        System.out.print("  maximizar  ");
        for (int j = 0; j < origN; j++) {
            if (j == 0) System.out.printf("%.4f·x_%d", origc[j], j + 1);
            else        System.out.printf(" %+.4f·x_%d", origc[j], j + 1);
        }
        System.out.println();
        System.out.println("  sujeito a:");
        for (int i = 0; i < origM; i++) {
            System.out.print("    ");
            for (int j = 0; j < origN; j++) {
                if (j == 0) System.out.printf("%.4f·x_%d", origA[i][j], j + 1);
                else        System.out.printf(" %+.4f·x_%d", origA[i][j], j + 1);
            }
            System.out.printf(" <= %.4f%n", origb[i]);
        }
        System.out.println("    x_j >= 0  para todo j");
    }

}
