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
 *
 * -----------------------------------------------------------------------------
 * VISUALIZAÇÃO
 * -----------------------------------------------------------------------------
 * Em vez de imprimir cada passo no terminal, esta classe registra um "histórico"
 * (lista de Step) com o estado completo da forma slack antes de cada pivô, além
 * de qual variável entra/sai e o teste da razão. Esse histórico é exportado como
 * JSON (método exportJson) e renderizado por SimplexVisualizer em uma página
 * HTML interativa (ver SimplexDemo).
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

    // PL original, guardado apenas para exibição/exportação
    private double[][] problemA;
    private double[] problemB;
    private double[] problemC;

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

    /** true se foi necessário resolver o PL auxiliar (Fase I) */
    private boolean phaseIUsed;

    /** Histórico de passos (snapshots da forma slack) para visualização */
    private List<Step> steps;

    /** Resultado da última chamada a solve() */
    private Result lastResult;

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
    // Snapshot de um passo do algoritmo (estado da forma slack + pivô seguinte)
    // -------------------------------------------------------------------------

    public static class Step {
        String title;
        String description;

        int[] N;
        int[] B;
        double[][] A;
        double[] b;
        double[] c;
        double v;

        // Pivô que leva deste passo para o próximo (null no último passo)
        Integer entering;  // variável que entra na base (índice global)
        Integer leaving;   // variável que sai da base (índice global)
        Integer pivotRow;  // posição em B da variável que sai
        Integer pivotCol;  // posição em N da variável que entra
        double[] ratios;   // Δ_i do teste da razão (mesma ordem de B); null se não aplicável

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"title\":").append(jsonStr(title)).append(",");
            sb.append("\"description\":").append(jsonStr(description)).append(",");
            sb.append("\"N\":").append(jsonArr(N)).append(",");
            sb.append("\"B\":").append(jsonArr(B)).append(",");
            sb.append("\"A\":").append(jsonMatrix(A)).append(",");
            sb.append("\"b\":").append(jsonArr(b)).append(",");
            sb.append("\"c\":").append(jsonArr(c)).append(",");
            sb.append("\"v\":").append(jsonNum(v)).append(",");
            sb.append("\"entering\":").append(jsonNullableInt(entering)).append(",");
            sb.append("\"leaving\":").append(jsonNullableInt(leaving)).append(",");
            sb.append("\"pivotRow\":").append(jsonNullableInt(pivotRow)).append(",");
            sb.append("\"pivotCol\":").append(jsonNullableInt(pivotCol)).append(",");
            sb.append("\"ratios\":").append(ratios == null ? "null" : jsonArr(ratios));
            sb.append("}");
            return sb.toString();
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

    /** Registra um snapshot do estado atual da forma slack no histórico */
    private void recordStep(String title, String description, Integer entering, Integer leaving,
                             Integer pivotRow, Integer pivotCol, double[] ratios) {
        Step s = new Step();
        s.title = title;
        s.description = description;
        s.N = N.clone();
        s.B = B.clone();
        s.A = deepCopy(A);
        s.b = b.clone();
        s.c = c.clone();
        s.v = v;
        s.entering = entering;
        s.leaving = leaving;
        s.pivotRow = pivotRow;
        s.pivotCol = pivotCol;
        s.ratios = ratios;
        steps.add(s);
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
    // LOOP PRINCIPAL DO SIMPLEX  (CLRS, p. 871, linhas 3–12)
    // =========================================================================
    /**
     * Executa iterações do Simplex (regra de Bland) a partir do estado atual de
     * N, B, A, b, c, v, registrando um Step ANTES de cada pivô — esse Step mostra
     * a forma slack corrente e já indica qual variável vai entrar/sair e qual o
     * elemento de pivô, para permitir destacar isso na visualização.
     *
     * O último Step registrado (quando ótimo ou ilimitado) não tem pivô seguinte.
     *
     * @param phasePrefix prefixo usado nos títulos dos passos ("Fase I — ", "Fase II — " ou "")
     * @return true se a solução ótima foi encontrada, false se o PL é ilimitado
     */
    private boolean runSimplexLoop(String phasePrefix) {
        int iteration = 0;
        while (true) {
            iteration++;

            // Escolha da variável ENTRANTE 'e' pela regra de Bland:
            // menor índice j em N com c_j > 0 (garante terminação, sem ciclagem)
            int e = -1;
            int smallestE = Integer.MAX_VALUE;
            for (int j = 0; j < N.length; j++) {
                if (c[j] > EPS && N[j] < smallestE) {
                    smallestE = N[j];
                    e = N[j];
                }
            }

            // Se não há c_j > 0, a solução atual é ÓTIMA
            if (e == -1) {
                recordStep(phasePrefix + "Solução ótima",
                        "Nenhum coeficiente de c é positivo: a solução básica atual já é ótima.",
                        null, null, null, null, null);
                return true;
            }

            int ePos = pos(N, e);

            // Teste da razão: Δ_i = b_i / a_{ie} para cada i em B com a_{ie} > 0
            double[] ratios = new double[B.length];
            int l = -1;
            double minDelta = Double.POSITIVE_INFINITY;
            int smallestL = Integer.MAX_VALUE;

            for (int i = 0; i < B.length; i++) {
                if (A[i][ePos] > EPS) {
                    ratios[i] = b[i] / A[i][ePos];
                    // Regra de Bland para desempate: menor índice entre empates
                    if (ratios[i] < minDelta - EPS ||
                            (Math.abs(ratios[i] - minDelta) < EPS && B[i] < smallestL)) {
                        minDelta = ratios[i];
                        l = B[i];
                        smallestL = B[i];
                    }
                } else {
                    ratios[i] = Double.POSITIVE_INFINITY;
                }
            }

            // Se Δ_i = ∞ para todo i, o PL é ILIMITADO
            if (l == -1) {
                recordStep(phasePrefix + "Iteração " + iteration,
                        String.format(Locale.US,
                                "Variável entrante x_%d não possui razão mínima finita "
                                + "(coeficientes não-positivos na coluna): o PL é ilimitado.", e),
                        e, null, null, ePos, ratios);
                return false;
            }

            int lPos = pos(B, l);
            recordStep(phasePrefix + "Iteração " + iteration,
                    String.format(Locale.US,
                            "Variável entrante: x_%d (regra de Bland). "
                            + "Variável sainte: x_%d (razão mínima Δ = %.4f).", e, l, minDelta),
                    e, l, lPos, ePos, ratios);

            pivot(l, e);
        }
    }

    // =========================================================================
    // PROCEDIMENTO SIMPLEX PRINCIPAL  (CLRS, p. 871)
    // =========================================================================
    /**
     * SIMPLEX(A, b, c)  — Algoritmo Simplex conforme CLRS.
     *
     * Recebe o PL na forma padrão e retorna a solução ótima (se existir).
     * O histórico de passos fica disponível em getSteps()/exportJson() para
     * visualização em HTML.
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
        problemA = origA;
        problemB = origb;
        problemC = origc;
        steps = new ArrayList<>();

        // CLRS linha 1: Inicializa a forma slack (ou detecta infeasibilidade)
        boolean feasible = initializeSimplex(origA, origb, origc);
        if (!feasible) {
            lastResult = new Result(Result.Status.INFEASIBLE, 0, null);
            return lastResult;
        }

        // CLRS linhas 3–12: Loop principal
        boolean optimal = runSimplexLoop(phaseIUsed ? "Fase II — " : "");
        if (!optimal) {
            lastResult = new Result(Result.Status.UNBOUNDED, 0, null);
            return lastResult;
        }

        // CLRS linhas 13–17: Extrai solução ótima
        // x_i = b[pos(B,i)] se i está na base, senão 0
        double[] x = new double[origN];
        for (int i = 1; i <= origN; i++) {
            int bPos = pos(B, i);
            x[i - 1] = (bPos != -1) ? b[bPos] : 0.0;
        }

        lastResult = new Result(Result.Status.OPTIMAL, v, x);
        return lastResult;
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
            phaseIUsed = false;

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
        // CASO 2: Precisa do PL auxiliar (Fase I)
        // -----------------------------------------------------------------
        phaseIUsed = true;

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

        recordStep("Fase I — PL auxiliar (forma aumentada)",
                String.format(Locale.US,
                        "b_%d = %.4f < 0: a origem não é viável. Introduzimos a variável "
                        + "artificial x_%d e fazemos um pivô forçado (x_%d sai, x_%d entra) "
                        + "para obter uma base viável do PL auxiliar Laux: maximizar -x_%d.",
                        B[kMin], origb[kMin], x0, B[kMin], x0, x0),
                x0, B[kMin], kMin, pos(N, x0), null);

        // Pivô inicial: x_0 entra, B[kMin] sai (torna solução viável para Laux)
        pivot(B[kMin], x0);

        // ---- Loop Simplex para Laux ----
        runSimplexLoop("Fase I — ");

        // Verifica viabilidade: ótimo de Laux deve ser 0 (x_0 = 0)
        if (v < -EPS) {
            recordStep("PL infeasível",
                    String.format(Locale.US,
                            "O ótimo do PL auxiliar é %.6f < 0, ou seja, x_%d > 0 em qualquer "
                            + "solução viável de Laux. Logo, o PL original não admite solução viável.",
                            v, x0),
                    null, null, null, null, null);
            return false;
        }

        // Se x_0 ainda está na base (com valor 0), pivota-a para fora
        int x0Pos = pos(B, x0);
        if (x0Pos != -1) {
            // Busca qualquer var não-básica j com A[x0Pos][j] ≠ 0 para pivot degenerado
            for (int j = 0; j < N.length; j++) {
                if (N[j] != x0 && Math.abs(A[x0Pos][j]) > EPS) {
                    recordStep("Fase I — Removendo variável artificial da base",
                            String.format(Locale.US,
                                    "x_%d = 0 ainda está na base. Pivô degenerado para retirá-la "
                                    + "(x_%d entra no lugar de x_%d).",
                                    x0, N[j], x0),
                            N[j], x0, x0Pos, j, null);
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
    // Exportação para visualização HTML
    // =========================================================================

    /** @return histórico de passos da última chamada a solve() */
    public List<Step> getSteps() {
        return steps;
    }

    /** @return resultado da última chamada a solve() */
    public Result getResult() {
        return lastResult;
    }

    /**
     * Serializa o PL, o histórico de passos e o resultado da última chamada a
     * solve() como um objeto JSON, para ser consumido por SimplexVisualizer.
     *
     * @param label rótulo do exemplo (exibido na página HTML)
     */
    public String exportJson(String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"label\":").append(jsonStr(label)).append(",");

        sb.append("\"problem\":{");
        sb.append("\"n\":").append(origN).append(",");
        sb.append("\"m\":").append(origM).append(",");
        sb.append("\"c\":").append(jsonArr(problemC)).append(",");
        sb.append("\"A\":").append(jsonMatrix(problemA)).append(",");
        sb.append("\"b\":").append(jsonArr(problemB));
        sb.append("},");

        sb.append("\"steps\":[");
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(steps.get(i).toJson());
        }
        sb.append("],");

        sb.append("\"result\":{");
        sb.append("\"status\":").append(jsonStr(lastResult.status.name())).append(",");
        sb.append("\"value\":").append(jsonNum(lastResult.optimalValue)).append(",");
        sb.append("\"solution\":").append(lastResult.solution == null ? "null" : jsonArr(lastResult.solution));
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers de serialização JSON (sem dependências externas)
    // -------------------------------------------------------------------------

    static String jsonNum(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) return "null";
        if (d == 0) d = 0; // normaliza -0.0
        return String.valueOf(d);
    }

    static String jsonArr(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonNum(arr[i]));
        }
        return sb.append("]").toString();
    }

    static String jsonArr(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        return sb.append("]").toString();
    }

    static String jsonMatrix(double[][] mat) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < mat.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonArr(mat[i]));
        }
        return sb.append("]").toString();
    }

    static String jsonStr(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                default:   sb.append(ch);
            }
        }
        return sb.append("\"").toString();
    }

    static String jsonNullableInt(Integer v) {
        return v == null ? "null" : String.valueOf(v);
    }
}
