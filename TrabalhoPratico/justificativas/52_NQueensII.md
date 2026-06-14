# 52. N-Queens II — Justificativa

**Estratégia:** Backtracking (busca em profundidade no espaço de soluções)
com poda via bitmask.

**Modelagem:** como duas rainhas nunca podem ficar na mesma linha, basta
decidir, para cada linha de 0 a n-1, em qual coluna colocar a rainha
daquela linha. Isso reduz o espaço de busca de `2^(n²)` (todas as
combinações de casas) para no máximo `n^n` (uma coluna por linha), e a poda
reduz ainda mais na prática.

**Representação por bitmask:** três inteiros guardam, em cada posição de
bit, se uma coluna (`cols`), uma diagonal principal (`diag1`) ou uma
diagonal secundária (`diag2`) já está "sob ataque" pelas rainhas colocadas
até a linha atual. A operação `((1 << n) - 1) & ~(cols | diag1 | diag2)`
calcula em O(1) o conjunto de colunas livres para a linha atual.

**Poda (branch and bound):** ao avançar para a próxima linha, atualiza-se
`cols`, `diag1` (deslocada à esquerda) e `diag2` (deslocada à direita) para
refletir as novas casas atacadas. Se, em alguma linha, `available == 0`, o
ramo é descartado imediatamente — não há necessidade de continuar
explorando colocações que já violam as restrições do problema. Quando
`row == n`, uma solução válida completa foi encontrada e o contador é
incrementado.

**Complexidade:**
- Tempo: no pior caso O(N!), mas a poda elimina a grande maioria dos ramos
  inválidos antes de chegar às últimas linhas (na prática, muito mais
  rápido que a enumeração ingênua).
- Espaço: O(N) para a profundidade da recursão (não há estruturas
  adicionais de tamanho O(N²)).
