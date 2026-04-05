/**
 * Classe de demonstração para o algoritmo Simplex.
 *
 * Mantém exemplos de uso separados da implementação principal em Simplex.java.
 */
public class SimplexDemo {

    private static void printHeader(String title) {
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println(title);
        System.out.println("══════════════════════════════════════════════════════");
    }

    public static void main(String[] args) {

        // ===================================================================
        // EXEMPLO 1 — Exemplo principal do CLRS (p. 848, eq. 29.53–29.57)
        //
        // maximizar   3x1 + x2 + 2x3
        // sujeito a:
        //    x1 +  x2 + 3x3 <= 30
        //   2x1 + 2x2 + 5x3 <= 24
        //   4x1 +  x2 + 2x3 <= 36
        //   x1, x2, x3 >= 0
        //
        // Solução ótima esperada: x1=8, x2=4, x3=0, z*=28
        // ===================================================================
        printHeader("EXEMPLO 1: Exemplo CLRS Seção 29.3");

        double[][] A1 = {
            {1, 1, 3},
            {2, 2, 5},
            {4, 1, 2}
        };
        double[] b1 = {30, 24, 36};
        double[] c1 = {3, 1, 2};

        new Simplex().solve(A1, b1, c1);

        System.out.println();

        // ===================================================================
        // EXEMPLO 2 — PL com solução básica inicial INVIÁVEL (b_i < 0)
        //
        // maximizar   x1 + x2
        // sujeito a:
        //   -x1 - x2 <= -3   (equivale a x1+x2 >= 3)
        //    x1 - x2 <= 1
        //    x1      <= 4
        //    x1, x2 >= 0
        //
        // Solução ótima esperada: x1=4, x2=3, z*=7
        // ===================================================================
        printHeader("EXEMPLO 2: Inicialização com PL Auxiliar (b_i < 0)");

        double[][] A2 = {
            {-1, -1},
            { 1, -1},
            { 1,  0}
        };
        double[] b2 = {-3, 1, 4};
        double[] c2 = {1, 1};

        new Simplex().solve(A2, b2, c2);

        System.out.println();

        // ===================================================================
        // EXEMPLO 3 — PL INFEASÍVEL
        //
        // maximizar   x1 + x2
        // sujeito a:
        //    x1 +  x2 <= 1
        //   -x1 - x2  <= -2   (equivale a x1+x2 >= 2)
        //   x1, x2 >= 0
        // ===================================================================
        printHeader("EXEMPLO 3: PL Infeasível");

        double[][] A3 = {
            { 1,  1},
            {-1, -1}
        };
        double[] b3 = {1, -2};
        double[] c3 = {1, 1};

        new Simplex().solve(A3, b3, c3);

        System.out.println();

        // ===================================================================
        // EXEMPLO 4 — PL ILIMITADO
        //
        // maximizar   x1 + x2
        // sujeito a:
        //   x1 - x2 <= 1
        //   x1, x2 >= 0
        // ===================================================================
        printHeader("EXEMPLO 4: PL Ilimitado");

        double[][] A4 = {
            {1, -1}
        };
        double[] b4 = {1};
        double[] c4 = {1, 1};

        new Simplex().solve(A4, b4, c4);
    }
}
