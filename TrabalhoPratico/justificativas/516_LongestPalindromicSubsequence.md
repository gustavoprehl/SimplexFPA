# 516. Longest Palindromic Subsequence — Justificativa

**Estratégia:** Programação Dinâmica em intervalos (interval DP).

**Definição do subproblema:** `dp[i][j]` representa o comprimento da maior
subsequência palíndroma contida em `s[i..j]` (substring de `i` a `j`,
inclusive).

**Subestrutura ótima (recorrência):** compara-se os caracteres das
extremidades do intervalo:
- Se `s[i] == s[j]`, ambos os caracteres podem fazer parte da
  subsequência palíndroma (um na ponta esquerda, outro na direita), e o
  problema se reduz a encontrar a maior subsequência palíndroma do
  intervalo interno `s[i+1..j-1]`, somando 2:
  `dp[i][j] = dp[i+1][j-1] + 2`.
- Se `s[i] != s[j]`, pelo menos um dos dois extremos não pode pertencer à
  subsequência ótima, então a resposta é o melhor entre ignorar a
  extremidade esquerda ou a direita:
  `dp[i][j] = max(dp[i+1][j], dp[i][j-1])`.

**Caso base:** `dp[i][i] = 1`, pois um único caractere é sempre um
palíndromo de tamanho 1.

**Subproblemas sobrepostos e ordem de preenchimento:** `dp[i][j]` depende
apenas de subintervalos menores (`dp[i+1][j-1]`, `dp[i+1][j]`,
`dp[i][j-1]`), todos com `i` maior ou `j` menor que o intervalo atual.
Preenchendo `i` de `n-1` até `0` e, para cada `i`, `j` de `i+1` até `n-1`,
garante-se que toda dependência já foi calculada antes de ser usada
(programação dinâmica bottom-up).

**Resposta final:** `dp[0][n-1]`, o intervalo que cobre a string inteira.

**Complexidade:**
- Tempo: O(n²), um valor para cada par `(i, j)` com `i <= j`.
- Espaço: O(n²) para a tabela `dp`.
