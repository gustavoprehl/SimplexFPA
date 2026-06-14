# 198. House Robber — Justificativa

**Estratégia:** Programação Dinâmica (bottom-up, com otimização de espaço).

**Subestrutura ótima:** seja `dp[i]` o valor máximo que se pode roubar
considerando apenas as primeiras `i` casas. Para a casa `i`, há exatamente
duas opções:
- **não roubar a casa `i`**: o melhor valor é `dp[i-1]`;
- **roubar a casa `i`**: como a casa `i-1` não pode ter sido roubada, o
  melhor valor é `dp[i-2] + nums[i]`.

Logo, a recorrência é:

```
dp[i] = max(dp[i-1], dp[i-2] + nums[i])
```

A solução ótima global é construída a partir das soluções ótimas de
subproblemas menores (prefixos do array), o que caracteriza a propriedade de
**subestrutura ótima**.

**Subproblemas sobrepostos:** `dp[i-1]` e `dp[i-2]` são reutilizados
repetidamente no cálculo de `dp[i]`, `dp[i+1]`, etc. Em vez de recalcular
esses valores recursivamente (o que levaria a complexidade exponencial), a
abordagem bottom-up resolve cada subproblema uma única vez, em ordem
crescente de `i`, armazenando o resultado.

**Otimização de espaço:** como `dp[i]` depende apenas dos dois valores
anteriores, não é necessário manter o vetor `dp` inteiro — basta guardar
`prev` (= `dp[i-2]`) e `curr` (= `dp[i-1]`), atualizando-os a cada iteração.

**Complexidade:**
- Tempo: O(n), uma única passagem pelo array.
- Espaço: O(1), apenas duas variáveis auxiliares.
