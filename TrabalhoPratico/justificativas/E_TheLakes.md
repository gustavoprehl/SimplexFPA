# E. The Lakes — Justificativa

**Estratégia:** Componentes conexas em grade (BFS), com leitura rápida e
fila explícita por questões de desempenho.

**Modelagem:** cada célula com `a[i][j] > 0` é um vértice de um grafo
implícito, com arestas para os vizinhos ortogonais (cima, baixo, esquerda,
direita) que também tenham valor `> 0`. Um "lago" é exatamente uma
componente conexa desse grafo, e seu "volume" é a soma dos valores dos
vértices da componente. O problema pede o maior volume entre todas as
componentes — ou 0 se não existir nenhuma célula com valor `> 0`.

**Algoritmo:** percorre-se cada célula da grade; se ainda não foi
visitada e tem valor `> 0`, executa-se uma BFS a partir dela, somando os
valores de todas as células alcançadas (marcando-as como visitadas para
não serem reprocessadas). O maior valor de soma encontrado entre todas as
BFS é a resposta.

**Por que BFS iterativa com array em vez de DFS recursiva:**
Como `n, m <= 1000` e a soma de `n*m` pode chegar a 10⁶, uma única
componente pode ter até 10⁶ células. Uma DFS recursiva nesse cenário
atingiria profundidade de recursão da ordem de 10⁶, estourando a pilha de
chamadas (StackOverflowError). A BFS com fila implementada como array de
tamanho `n*m` evita esse problema, processando cada célula em O(1)
amortizado.

**Por que leitura rápida (StreamTokenizer):**
Com `t <= 10^4` casos de teste e soma de `n*m <= 10^6`, o volume total de
números a serem lidos pode chegar à ordem de milhões. `Scanner` é lento
demais para essa escala (risco de Time Limit Exceeded); `StreamTokenizer`
sobre um `BufferedInputStream` realiza a tokenização de forma eficiente,
em tempo linear no tamanho da entrada.

**Complexidade (por caso de teste):**
- Tempo: O(n·m) — cada célula é visitada e processada uma única vez.
- Espaço: O(n·m) para os vetores `a`, `visited` e `queue`.

Como a soma de `n·m` sobre todos os casos de teste é limitada a 10⁶, o
tempo total do programa é O(10⁶), dentro do limite de 3 segundos.
