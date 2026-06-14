# 392. Is Subsequence — Justificativa

**Estratégia:** Dois ponteiros (técnica gulosa).

A solução percorre `s` (ponteiro `i`) e `t` (ponteiro `j`) simultaneamente.
Sempre que `s[i] == t[j]`, o caractere de `s` é "consumido" (`i++`); o ponteiro
`j` avança a cada iteração, independentemente de ter havido correspondência.
`s` é subsequência de `t` se, ao final, `i` percorreu todos os caracteres de `s`.

**Por que essa escolha gulosa é ótima (correção):**
Dado um caractere `s[i]` e a primeira ocorrência possível dele em `t` a partir
da posição `j`, casar `s[i]` com essa primeira ocorrência nunca é pior do que
casá-lo com uma ocorrência posterior. Isso porque qualquer correspondência
válida para o restante de `s` que use uma posição `j' > j` também é válida
(ou mais restrita) se usarmos `j`, já que sobra mais "espaço" em `t` à
direita. Portanto, a escolha gulosa de avançar assim que há correspondência
preserva a existência de uma solução completa, sem necessidade de
backtracking.

**Complexidade:**
- Tempo: O(|t|), pois cada ponteiro avança no máximo `|t|` vezes.
- Espaço: O(1), apenas dois índices inteiros.

**Follow-up (k ≥ 10⁹ consultas sobre o mesmo `t`):**
Pré-processar `t` uma única vez, construindo, para cada caractere `c`, a
lista ordenada `pos[c]` com os índices onde `c` ocorre em `t` — O(|t|).
Para cada nova string `s_i`, percorrer seus caracteres e, para cada um,
fazer busca binária em `pos[c]` pela menor posição estritamente maior que a
posição atual em `t`. Se não existir tal posição para algum caractere, `s_i`
não é subsequência. Isso reduz o custo por consulta de O(|t|) para
O(|s_i| · log|t|), o que é essencial quando `t` é fixo e há um número muito
grande de consultas.
