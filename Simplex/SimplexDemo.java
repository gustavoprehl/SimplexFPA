import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Classe de demonstração para o algoritmo Simplex.
 *
 * Executa os exemplos e gera "simplex_visualization.html": uma página
 * interativa que permite navegar passo a passo pelas iterações de cada
 * exemplo (em vez da antiga visualização impressa no terminal).
 */
public class SimplexDemo {

    private static void printSummary(String label, Simplex.Result r) {
        System.out.println(label);
        switch (r.status) {
            case OPTIMAL:
                System.out.printf(Locale.US, "  OPTIMAL  z* = %.4f%n", r.optimalValue);
                for (int i = 0; i < r.solution.length; i++)
                    System.out.printf(Locale.US, "  x_%d = %.4f%n", i + 1, r.solution[i]);
                break;
            case INFEASIBLE:
                System.out.println("  INFEASIBLE");
                break;
            case UNBOUNDED:
                System.out.println("  UNBOUNDED");
                break;
        }
        System.out.println();
    }

    public static void main(String[] args) throws IOException {

        List<String> caseJsons = new ArrayList<>();

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
        double[][] A1 = {
            {1, 1, 3},
            {2, 2, 5},
            {4, 1, 2}
        };
        double[] b1 = {30, 24, 36};
        double[] c1 = {3, 1, 2};

        Simplex s1 = new Simplex();
        printSummary("EXEMPLO 1: Exemplo CLRS Seção 29.3", s1.solve(A1, b1, c1));
        caseJsons.add(s1.exportJson("Exemplo 1: Exemplo CLRS Seção 29.3"));

        // ===================================================================
        // EXEMPLO 2 — PL com solução básica inicial INVIÁVEL (b_i < 0)
        //
        // maximizar   2x1 + 3x2
        // sujeito a:
        //    x1 +  x2 <= 4
        //    x1 + 2x2 <= 6
        //   -x1 -  x2 <= -2   (equivale a x1+x2 >= 2)
        //    x1, x2 >= 0
        //
        // Solução ótima esperada: x1=2, x2=2, z*=10
        // ===================================================================
        double[][] A2 = {
            { 1,  1},
            { 1,  2},
            {-1, -1}
        };
        double[] b2 = {4, 6, -2};
        double[] c2 = {2, 3};

        Simplex s2 = new Simplex();
        printSummary("EXEMPLO 2: Inicialização com PL Auxiliar (b_i < 0)", s2.solve(A2, b2, c2));
        caseJsons.add(s2.exportJson("Exemplo 2: Inicialização com PL Auxiliar (b_i < 0)"));

        // ===================================================================
        // EXEMPLO 3 — PL INFEASÍVEL
        //
        // maximizar   x1 + x2
        // sujeito a:
        //    x1 +  x2 <= 1
        //   -x1 - x2  <= -2   (equivale a x1+x2 >= 2)
        //   x1, x2 >= 0
        // ===================================================================
        double[][] A3 = {
            { 1,  1},
            {-1, -1}
        };
        double[] b3 = {1, -2};
        double[] c3 = {1, 1};

        Simplex s3 = new Simplex();
        printSummary("EXEMPLO 3: PL Infeasível", s3.solve(A3, b3, c3));
        caseJsons.add(s3.exportJson("Exemplo 3: PL Infeasível"));

        // ===================================================================
        // EXEMPLO 4 — PL ILIMITADO
        //
        // maximizar   x1 + x2
        // sujeito a:
        //   x1 - x2 <= 1
        //   x1, x2 >= 0
        // ===================================================================
        double[][] A4 = {
            {1, -1}
        };
        double[] b4 = {1};
        double[] c4 = {1, 1};

        Simplex s4 = new Simplex();
        printSummary("EXEMPLO 4: PL Ilimitado", s4.solve(A4, b4, c4));
        caseJsons.add(s4.exportJson("Exemplo 4: PL Ilimitado"));

        String outputPath = "simplex_visualization.html";
        SimplexVisualizer.write(caseJsons, outputPath);
        System.out.println("Visualização gerada em: " + outputPath);
    }
}
