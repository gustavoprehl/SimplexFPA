# 654. Maximum Binary Tree — Justificativa

**Estratégia:** Pilha monotônica, construindo a árvore em uma única
passagem (em vez da abordagem ingênua O(n²) de buscar o máximo de cada
subarray recursivamente).

**Invariante da pilha:** a pilha mantém, do fundo para o topo, os nós que
ainda não encontraram, à sua direita, um valor maior que o seu — ou seja,
candidatos a serem "dominados" por um número futuro. Os valores na pilha
são estritamente decrescentes do fundo para o topo, e cada nó é o filho
direito do nó imediatamente abaixo dele (formando a "espinha direita" da
árvore parcial construída até o momento).

**Relação com a definição recursiva do problema:** na árvore binária
máxima, o pai de um elemento `nums[i]` é o **menor** entre o primeiro
elemento maior à sua esquerda e o primeiro elemento maior à sua direita
(o "dominador" mais próximo). Ao processar `nums` da esquerda para a
direita:
- Se o número atual é **maior** que o topo da pilha, ele é o primeiro
  elemento maior à direita de todos os nós removidos — portanto, o número
  atual se torna ancestral deles. O último nó removido (o de maior valor
  entre os removidos, e portanto o mais próximo na hierarquia) vira o
  **filho esquerdo** do novo nó; os demais já estavam encadeados como
  filhos direitos uns dos outros, preservando a ordem relativa original.
- Se a pilha não ficou vazia após as remoções, o topo restante é o
  primeiro elemento maior à esquerda do número atual — logo, o número
  atual se torna **filho direito** desse nó.

**Análise amortizada (custo O(n)):**
Cada elemento de `nums` é empilhado exatamente uma vez e desempilhado no
máximo uma vez durante toda a execução. Pelo método agregado de análise
amortizada, o número total de operações de pilha (push + pop) ao longo das
`n` iterações é, no máximo, `2n`, mesmo que o laço `while` interno execute
múltiplas vezes em iterações específicas.

**Complexidade:**
- Tempo: O(n) amortizado.
- Espaço: O(n) para a pilha (no pior caso, array estritamente crescente).
