# 2360. Longest Cycle in a Graph — Justificativa

**Estratégia:** Travessia iterativa de grafo funcional com marcação por
timestamp (variação de busca em profundidade sem recursão/pilha extra).

**Estrutura do problema:** como cada nó tem grau de saída no máximo 1, o
grafo é uma coleção de "componentes funcionais": cada um é uma cadeia de
nós que termina em `-1` (sem ciclo) ou que eventualmente entra em um único
ciclo (formato "ρ" / rho). Isso significa que, a partir de qualquer nó,
seguir os ponteiros `edges[i]` leva, no máximo, a um único ciclo.

**Algoritmo:** para cada nó `i` ainda não visitado, percorre-se a cadeia a
partir dele, atribuindo a cada nó visitado nessa travessia um `timestamp`
(identificador da travessia atual) e a sua distância (`dist`, número de
passos desde `i`). A travessia para quando:
- chega a `-1` (cadeia sem ciclo) — não há ciclo a contabilizar; ou
- chega a um nó já visitado:
  - se o `timestamp` desse nó for o **mesmo** da travessia atual, então o
    nó faz parte de um ciclo encontrado **agora**, e o tamanho do ciclo é
    `passos_atuais - dist[nó]` (a diferença entre quando o nó foi
    "reencontrado" e quando foi visitado pela primeira vez nesta mesma
    travessia);
  - se o `timestamp` for de uma travessia **anterior**, a cadeia atual
    apenas se conecta a um componente já processado, sem formar um novo
    ciclo.

**Por que cada nó é processado uma única vez (correção da complexidade):**
Uma vez que `visited[curr] != 0`, o nó nunca é revisitado por outra
travessia (a condição `visited[curr] == 0` no laço `while` impede isso).
Logo, cada nó é marcado e processado exatamente uma vez ao longo de todas
as travessias.

**Complexidade:**
- Tempo: O(n), pois cada nó é visitado uma única vez no total, somando
  todas as travessias.
- Espaço: O(n) para os vetores `visited` e `dist`.
