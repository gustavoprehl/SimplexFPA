# 1971. Find if Path Exists in Graph — Justificativa

**Estratégia:** Union-Find (Disjoint Set Union — DSU) com compressão de
caminho e união por rank.

**Modelagem:** existe um caminho entre `source` e `destination` se e
somente se os dois vértices pertencem ao mesmo componente conexo do grafo.
A estrutura DSU mantém uma partição dos vértices em conjuntos disjuntos,
onde cada conjunto representa um componente conexo, identificado por um
representante (raiz).

**Construção:** inicialmente cada vértice é seu próprio conjunto
(`parent[i] = i`). Para cada aresta `(u, v)` em `edges`, executa-se
`union(u, v)`, unindo os conjuntos aos quais `u` e `v` pertencem. Ao final,
basta verificar se `find(source) == find(destination)`.

**Compressão de caminho (`find`):** ao buscar a raiz de um elemento, cada
nó visitado é reconectado mais perto da raiz (aqui via *path halving*:
`parent[x] = parent[parent[x]]`), achatando a árvore e acelerando buscas
futuras.

**União por rank (`union`):** ao unir dois conjuntos, a raiz da árvore de
menor `rank` (altura estimada) é pendurada na raiz da árvore de maior
`rank`, evitando que as árvores fiquem muito altas.

**Complexidade (teoria de DSU):** combinando compressão de caminho e união
por rank, cada operação `find`/`union` tem custo amortizado O(α(n)), onde
α é a inversa da função de Ackermann — na prática, uma constante muito
pequena (≤ 4 para qualquer `n` razoável).

- Tempo: O((n + |edges|) · α(n)) ≈ O(n + |edges|).
- Espaço: O(n) para os vetores `parent` e `rank_`.
