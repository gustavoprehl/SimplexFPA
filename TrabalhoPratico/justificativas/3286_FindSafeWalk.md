# 3286. Find a Safe Walk Through a Grid — Justificativa

**Estratégia:** Caminho mínimo em grafo com pesos {0, 1} — algoritmo
**0-1 BFS**, uma especialização do algoritmo de Dijkstra.

**Modelagem:** cada célula da grade é um vértice; há uma aresta entre
células adjacentes (4 direções). O "custo" de entrar em uma célula
`grid[i][j] = 1` é 1 (perde-se 1 ponto de vida) e o custo de uma célula
`grid[i][j] = 0` é 0. Definimos `dist[i][j]` como o menor número total de
células inseguras percorridas (incluindo a célula de partida) em qualquer
caminho de `(0,0)` até `(i,j)`. A resposta é "true" se e somente se
`dist[m-1][n-1] <= health - 1`, ou seja, se a vida restante ao chegar no
destino for pelo menos 1.

**Por que o 0-1 BFS é correto (relação com Dijkstra):**
O algoritmo de Dijkstra processa vértices em ordem não decrescente de
distância, garantindo que, quando um vértice é retirado da estrutura, sua
distância já é a definitiva (propriedade da escolha gulosa + relaxamento de
arestas). Como aqui os pesos das arestas são apenas 0 ou 1, um deque
substitui a fila de prioridade: ao relaxar uma aresta de peso 0, o vizinho é
inserido no início do deque (mesma "camada" de distância); ao relaxar uma
aresta de peso 1, o vizinho é inserido no final (próxima camada). Essa
invariante mantém o deque sempre ordenado por distância, preservando a
propriedade de Dijkstra sem necessidade de heap.

**Complexidade:**
- Tempo: O(m·n), pois cada célula é inserida/relaxada um número constante
  de vezes (grau ≤ 4).
- Espaço: O(m·n) para a matriz `dist` e o deque.
