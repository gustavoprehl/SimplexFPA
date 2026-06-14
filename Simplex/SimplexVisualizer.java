import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Gera uma página HTML autocontida (sem dependências externas) que visualiza,
 * passo a passo, a execução do algoritmo Simplex registrada por {@link Simplex}.
 *
 * Cada chamada a {@link Simplex#exportJson(String)} produz o JSON de um "caso"
 * (PL + histórico de passos + resultado). Esta classe junta esses JSONs em um
 * array e os injeta em um template HTML/CSS/JS que permite navegar entre os
 * passos (botões anterior/próximo, seletor de passo) destacando, em cada
 * tableau, a variável que entra/sai da base e o elemento de pivô.
 */
public class SimplexVisualizer {

    /**
     * Escreve a página de visualização em {@code outputPath}.
     *
     * @param caseJsons lista de JSONs (um por exemplo), cada um produzido por
     *                  {@link Simplex#exportJson(String)}
     * @param outputPath caminho do arquivo .html a ser gerado
     */
    public static void write(List<String> caseJsons, String outputPath) throws IOException {
        StringBuilder data = new StringBuilder("[");
        for (int i = 0; i < caseJsons.size(); i++) {
            if (i > 0) data.append(",");
            data.append(caseJsons.get(i));
        }
        data.append("]");

        String html = TEMPLATE.replace("\"__SIMPLEX_DATA__\"", data.toString());
        Files.writeString(Path.of(outputPath), html, StandardCharsets.UTF_8);
    }

    private static final String TEMPLATE = """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Visualizador do Algoritmo Simplex</title>
        <style>
        :root {
          --pivot-col: #fff3bf;
          --pivot-row: #d0ebff;
          --pivot-cell: #ffa94d;
          --border: #ccc;
        }
        * { box-sizing: border-box; }
        body {
          font-family: 'Segoe UI', Arial, sans-serif;
          background: #f4f6f8;
          color: #222;
          margin: 0;
          padding: 20px;
        }
        .container { max-width: 920px; margin: 0 auto; }
        h1 { margin-bottom: 4px; }
        .subtitle { color: #666; margin-top: 0; }
        .card {
          background: #fff;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          padding: 16px 20px;
          margin-bottom: 16px;
          box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .card h2 { margin-top: 0; font-size: 18px; }
        .card h3 { margin-bottom: 6px; font-size: 15px; }
        select, button {
          font-size: 14px;
          padding: 6px 10px;
          border-radius: 4px;
          border: 1px solid #bbb;
          background: #fff;
          cursor: pointer;
        }
        button:disabled { opacity: 0.4; cursor: default; }
        .nav { display: flex; gap: 10px; align-items: center; margin-bottom: 12px; flex-wrap: wrap; }
        .nav select { flex: 1; min-width: 200px; }
        .step-counter { color: #888; font-size: 13px; }

        .problem-line { margin: 4px 0; font-size: 16px; }
        .legend { margin-top: 10px; font-size: 13px; color: #555; }

        .table-scroll { overflow-x: auto; }
        table.tableau {
          border-collapse: collapse;
          margin-top: 10px;
          width: 100%;
          font-size: 15px;
        }
        table.tableau th, table.tableau td {
          border: 1px solid var(--border);
          padding: 6px 12px;
          text-align: center;
          min-width: 70px;
          white-space: nowrap;
        }
        table.tableau th { background: #f0f2f5; }
        .rowlabel { text-align: left; font-weight: bold; background: #f8f9fa; }
        .col-highlight { background: var(--pivot-col); }
        .row-highlight { background: var(--pivot-row); }
        .pivot-cell { background: var(--pivot-cell); font-weight: bold; outline: 2px solid #d9480f; outline-offset: -2px; }

        table.ratios { border-collapse: collapse; margin-top: 6px; font-size: 14px; }
        table.ratios th, table.ratios td { border: 1px solid var(--border); padding: 6px 12px; text-align: center; }
        table.ratios th { background: #f0f2f5; }
        table.ratios tr.chosen td { background: var(--pivot-row); font-weight: bold; }

        .result-box { margin-top: 14px; padding: 12px 16px; border-radius: 6px; font-weight: bold; }
        .result-optimal { background: #ebfbee; border: 1px solid #2f9e44; color: #2b8a3e; }
        .result-infeasible, .result-unbounded { background: #fff5f5; border: 1px solid #e03131; color: #c92a2a; }

        .footer { text-align: center; color: #888; font-size: 13px; margin-top: 8px; }
        </style>
        </head>
        <body>
        <div class="container">
          <h1>Visualizador do Algoritmo Simplex</h1>
          <p class="subtitle">Baseado em CLRS (Cormen, Leiserson, Rivest, Stein), Cap. 29 — Programação Linear</p>

          <div class="card">
            <label for="caseSelect"><strong>Exemplo:</strong></label>
            <select id="caseSelect"></select>
          </div>

          <div class="card" id="problemCard">
            <h2>PL na forma padrão</h2>
            <div id="problemBody"></div>
            <div id="legend" class="legend"></div>
          </div>

          <div class="card">
            <div class="nav">
              <button id="prevBtn">&laquo; Anterior</button>
              <select id="stepSelect"></select>
              <button id="nextBtn">Próximo &raquo;</button>
              <span class="step-counter" id="stepCounter"></span>
            </div>
            <h2 id="stepTitle"></h2>
            <p id="stepDescription"></p>

            <div id="tableauWrap"></div>
            <div id="ratioWrap"></div>
            <div id="resultWrap"></div>
          </div>

          <p class="footer">Use as setas &larr; / &rarr; do teclado para navegar entre os passos.</p>
        </div>

        <script>
        const CASES = "__SIMPLEX_DATA__";

        const caseSelect = document.getElementById('caseSelect');
        const stepSelect = document.getElementById('stepSelect');
        const prevBtn = document.getElementById('prevBtn');
        const nextBtn = document.getElementById('nextBtn');
        const stepCounter = document.getElementById('stepCounter');

        let caseIdx = 0;
        let stepIdx = 0;

        function fmt(x) {
          let s = x.toFixed(4);
          if (s === '-0.0000') s = '0.0000';
          return s;
        }

        function fmtSigned(x) {
          const s = fmt(x);
          return s.startsWith('-') ? s : '+' + s;
        }

        function varLabel(idx) {
          return 'x<sub>' + idx + '</sub>';
        }

        function linExpr(coeffs, startIdx) {
          let html = '';
          for (let j = 0; j < coeffs.length; j++) {
            const val = coeffs[j];
            const term = fmt(Math.abs(val)) + '&middot;' + varLabel(startIdx + j);
            if (j === 0) {
              html += (val < 0 ? '-' : '') + term;
            } else {
              html += (val < 0 ? ' - ' : ' + ') + term;
            }
          }
          return html;
        }

        function tdClass(isPivot, isRow, isCol) {
          if (isPivot) return ' class="pivot-cell"';
          if (isRow) return ' class="row-highlight"';
          if (isCol) return ' class="col-highlight"';
          return '';
        }

        function renderProblem() {
          const c = CASES[caseIdx];
          const problem = c.problem;
          const n = problem.n, m = problem.m;

          let html = '<div class="problem-line"><strong>maximizar</strong>&nbsp;&nbsp;' + linExpr(problem.c, 1) + '</div>';
          html += '<div class="problem-line"><strong>sujeito a:</strong></div>';
          for (let i = 0; i < m; i++) {
            html += '<div class="problem-line">&nbsp;&nbsp;' + linExpr(problem.A[i], 1) + ' &le; ' + fmt(problem.b[i]) + '</div>';
          }
          html += '<div class="problem-line">&nbsp;&nbsp;' + varLabel('j') + ' &ge; 0 para todo j</div>';
          document.getElementById('problemBody').innerHTML = html;

          const hasArtificial = c.steps.some(s => s.N.includes(n + m + 1) || s.B.includes(n + m + 1));
          let legend = varLabel(1) + '..' + varLabel(n) + ' = decisão, ';
          legend += varLabel(n + 1) + '..' + varLabel(n + m) + ' = folga (slack)';
          if (hasArtificial) legend += ', ' + varLabel(n + m + 1) + ' = artificial (Fase I)';
          document.getElementById('legend').innerHTML = legend;
        }

        function renderTableau(step) {
          const N = step.N, B = step.B, A = step.A, b = step.b, c = step.c, v = step.v;
          const pr = step.pivotRow, pc = step.pivotCol;

          let html = '<div class="table-scroll"><table class="tableau"><thead><tr><th class="rowlabel">base</th><th>valor</th>';
          for (let j = 0; j < N.length; j++) {
            html += '<th' + tdClass(false, false, pc === j) + '>' + varLabel(N[j]) + '</th>';
          }
          html += '</tr></thead><tbody>';

          html += '<tr><td class="rowlabel">z</td><td>' + fmt(v) + '</td>';
          for (let j = 0; j < N.length; j++) {
            html += '<td' + tdClass(false, false, pc === j) + '>' + fmtSigned(c[j]) + '</td>';
          }
          html += '</tr>';

          for (let i = 0; i < B.length; i++) {
            const rowHighlight = (pr === i) ? ' row-highlight' : '';
            html += '<tr><td class="rowlabel' + rowHighlight + '">' + varLabel(B[i]) + '</td>';
            html += '<td' + tdClass(false, pr === i, false) + '>' + fmt(b[i]) + '</td>';
            for (let j = 0; j < N.length; j++) {
              html += '<td' + tdClass(pr === i && pc === j, pr === i, pc === j) + '>' + fmtSigned(-A[i][j]) + '</td>';
            }
            html += '</tr>';
          }
          html += '</tbody></table></div>';
          document.getElementById('tableauWrap').innerHTML = html;
        }

        function renderRatios(step) {
          const wrap = document.getElementById('ratioWrap');
          if (!step.ratios) { wrap.innerHTML = ''; return; }
          const B = step.B, A = step.A, b = step.b, ratios = step.ratios;
          const pc = step.pivotCol, pr = step.pivotRow;

          let html = '<h3>Teste da razão (variável entrante: ' + varLabel(step.N[pc]) + ')</h3>';
          html += '<table class="ratios"><thead><tr><th>Variável básica</th><th>a<sub>i,e</sub></th><th>b<sub>i</sub></th><th>&Delta;<sub>i</sub> = b<sub>i</sub> / a<sub>i,e</sub></th></tr></thead><tbody>';
          for (let i = 0; i < B.length; i++) {
            const rowCls = (pr === i) ? ' class="chosen"' : '';
            const ratioStr = (ratios[i] === null) ? '&mdash;' : fmt(ratios[i]);
            html += '<tr' + rowCls + '><td>' + varLabel(B[i]) + '</td><td>' + fmt(A[i][pc]) + '</td><td>' + fmt(b[i]) + '</td><td>' + ratioStr + '</td></tr>';
          }
          html += '</tbody></table>';
          wrap.innerHTML = html;
        }

        function renderResult(isLastStep) {
          const wrap = document.getElementById('resultWrap');
          if (!isLastStep) { wrap.innerHTML = ''; return; }
          const r = CASES[caseIdx].result;
          let html = '';
          if (r.status === 'OPTIMAL') {
            html += '<div class="result-box result-optimal">Solução ótima: z* = ' + fmt(r.value) + '<br>';
            for (let i = 0; i < r.solution.length; i++) {
              html += varLabel(i + 1) + ' = ' + fmt(r.solution[i]) + '&nbsp;&nbsp;&nbsp;';
            }
            html += '</div>';
          } else if (r.status === 'INFEASIBLE') {
            html += '<div class="result-box result-infeasible">PL infeasível: não existe solução viável (a região factível é vazia).</div>';
          } else if (r.status === 'UNBOUNDED') {
            html += '<div class="result-box result-unbounded">PL ilimitado: a função objetivo cresce indefinidamente na região factível.</div>';
          }
          wrap.innerHTML = html;
        }

        function renderStep() {
          const c = CASES[caseIdx];
          const s = c.steps[stepIdx];

          document.getElementById('stepTitle').textContent = s.title;
          document.getElementById('stepDescription').textContent = s.description;
          stepCounter.textContent = 'Passo ' + (stepIdx + 1) + ' de ' + c.steps.length;

          renderTableau(s);
          renderRatios(s);

          const isLast = (stepIdx === c.steps.length - 1);
          renderResult(isLast);

          stepSelect.value = String(stepIdx);
          prevBtn.disabled = (stepIdx === 0);
          nextBtn.disabled = isLast;
        }

        function renderCase() {
          const c = CASES[caseIdx];
          renderProblem();

          stepSelect.innerHTML = '';
          c.steps.forEach((s, i) => {
            const opt = document.createElement('option');
            opt.value = String(i);
            opt.textContent = (i + 1) + '. ' + s.title;
            stepSelect.appendChild(opt);
          });

          stepIdx = 0;
          renderStep();
        }

        CASES.forEach((c, i) => {
          const opt = document.createElement('option');
          opt.value = String(i);
          opt.textContent = c.label;
          caseSelect.appendChild(opt);
        });

        caseSelect.addEventListener('change', () => {
          caseIdx = parseInt(caseSelect.value, 10);
          renderCase();
        });
        stepSelect.addEventListener('change', () => {
          stepIdx = parseInt(stepSelect.value, 10);
          renderStep();
        });
        prevBtn.addEventListener('click', () => {
          if (stepIdx > 0) { stepIdx--; renderStep(); }
        });
        nextBtn.addEventListener('click', () => {
          if (stepIdx < CASES[caseIdx].steps.length - 1) { stepIdx++; renderStep(); }
        });
        document.addEventListener('keydown', (e) => {
          if (e.key === 'ArrowLeft') prevBtn.click();
          else if (e.key === 'ArrowRight') nextBtn.click();
        });

        renderCase();
        </script>
        </body>
        </html>
        """;
}
