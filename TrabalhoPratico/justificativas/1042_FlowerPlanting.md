# 1042. Flower Planting With No Adjacent — Justificativa

**Estratégia:** Coloração gulosa de grafos.

O problema é equivalente a colorir os vértices de um grafo (jardins =
vértices, caminhos = arestas) de forma que vértices adjacentes recebam cores
diferentes, usando até 4 cores (tipos de flores).

**Por que 4 cores sempre bastam (correção):**
A restrição garante que cada vértice tem grau no máximo 3 (no máximo 3
caminhos incidentes). Pelo teorema da coloração gulosa, qualquer grafo pode
ser colorido com no máximo `Δ + 1` cores, onde `Δ` é o grau máximo — basta
processar os vértices em qualquer ordem e, para cada um, atribuir a menor
cor que ainda não foi usada por nenhum de seus vizinhos já coloridos. Como
`Δ ≤ 3`, sempre haverá pelo menos uma cor livre entre as 4 disponíveis,
mesmo que todos os vizinhos já coloridos usem cores distintas entre si.

**Implementação:** constrói-se a lista de adjacência a partir de `paths` e,
para cada jardim de 1 a n, marca-se em um vetor booleano `used[1..4]` quais
cores já foram usadas pelos vizinhos processados anteriormente; a primeira
cor livre é atribuída ao jardim atual.

**Complexidade:**
- Tempo: O(n + |paths|) — construção do grafo é O(|paths|) e a coloração
  visita cada vértice e cada aresta um número constante de vezes (grau ≤ 3).
- Espaço: O(n + |paths|) para a lista de adjacência e o vetor de resultado.
