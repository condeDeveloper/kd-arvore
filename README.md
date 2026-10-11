# kd-arvore

Árvore k-d do zero em Java 21, contra a força bruta e contra a ideia ingênua de ordenar os
pontos por um eixo só.

O juiz é a **força bruta**, e ela é o juiz certo e não só o conveniente: não há nela nenhuma
invariante que possa quebrar, nenhum lado a escolher, nenhuma subárvore a descartar. Ela
calcula as n distâncias e ordena.

As coordenadas são inteiras e a distância é guardada **ao quadrado num long**, exata. Assim
"estes dois pontos empatam" é um fato e não uma tolerância, e o juiz pode comparar resposta
por resposta sem ruído.

A unidade é o **ponto visitado**, e ela tem um teto conhecido: n. Cada número das tabelas já
vem como fração do único valor que importa, então "visitou 19.751 de 20.000" é uma frase
completa.

## A maldição da dimensionalidade, medida

```
$ mvn exec:java -Dexec.args="dimensao"

   dimensoes       alvo do cubo    fracao de n     alvo de dentro    fracao de n podas por consulta
           2               17.4         0.0009               11.8         0.0006               11.7
           3               33.0         0.0017               10.8         0.0005               14.2
           4               73.6         0.0037               11.1         0.0006               19.5
           6              341.4         0.0171               12.5         0.0006               41.0
           8             1190.7         0.0595               11.3         0.0006               76.1
          10             4436.8         0.2218               31.4         0.0016              176.1
          12            13631.4         0.6816               14.9         0.0007              134.8
          16            19751.8         0.9876               13.2         0.0007               11.9
```

Com o alvo sorteado no cubo inteiro, a árvore passa de **17,4** pontos visitados em dimensão
2 para **19.751,8** em dimensão 16, que é 98,8% dos vinte mil. Ela deixa de ser uma
estrutura de busca e vira uma força bruta com ponteiros.

A causa não é da árvore nem da implementação, é do espaço. Em dimensão alta o vizinho mais
próximo não é muito mais próximo do que o décimo, nem do que o milésimo. O raio de poda
chega apertado em duas dimensões porque o vizinho está **muito** mais perto que o resto; em
dezesseis, não está, e um raio frouxo não descarta caixa nenhuma.

## A poda é o algoritmo inteiro

```
$ mvn exec:java -Dexec.args="poda"

   dimensoes versao               pontos visitados      fracao de n
           2 kd com poda                      17.4           0.0009
           2 kd sem poda                   20000.0           1.0000
           2 projecao                        139.6           0.0070
           2 forca bruta                   20000.0           1.0000
          16 kd com poda                   19751.8           0.9876
          16 kd sem poda                   20000.0           1.0000
          16 projecao                      17732.6           0.8866
          16 forca bruta                   20000.0           1.0000
```

**Sem poda, a árvore visita exatamente 20.000,0 pontos**, igual à força bruta, e ainda paga a
descida. A estrutura não economiza nada sozinha: ela organiza o espaço de um jeito que
*permite* descartar, e quem descarta é a comparação entre a distância ao plano de corte e o
raio do melhor vizinho de hoje.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `dimensao`, `poda`, `regra`, `alvo`, `kvizinhos`,
`folha` e `tudo`.

## O que as medidas me corrigiram

**A coluna que eu não tinha pensado em medir: de onde vem o alvo.**

A tabela da dimensão tem duas colunas de pontos visitados porque uma sozinha era mentira por
omissão. A primeira pergunta de um alvo sorteado no cubo inteiro. A segunda pergunta de um
alvo tirado de dentro da própria nuvem, com um empurrão pequeno.

Em dimensão 16, a mesma árvore visita **19.751,8** pontos ou **13,2**. Mil e quinhentas vezes
de diferença, e a única coisa que muda é de onde veio a pergunta.

Isso não desmente a maldição, delimita ela. O que fica caro em dimensão alta é perguntar por
um ponto que não se parece com nada do conjunto, e consulta de verdade quase nunca é assim:
quem pergunta "quem se parece com isto" já tem um isto que veio do mesmo lugar que os dados.

E a relação entre o alvo e a nuvem não é um interruptor de duas posições:

```
$ mvn exec:java -Dexec.args="alvo"

  nuvem        alvo               empurrao     pontos visitados      fracao de n
  uniforme     de dentro               500                 11.3           0.0006
  uniforme     de dentro             10000                 27.8           0.0014
  uniforme     de dentro            100000                411.3           0.0206
  uniforme     de dentro            500000               2579.9           0.1290
  uniforme     do cubo             1000000               1190.7           0.0595
  esticada     de dentro               500                 30.8           0.0015
  esticada     de dentro             10000                542.6           0.0271
  esticada     de dentro            100000               5423.4           0.2712
  esticada     de dentro            500000              16939.1           0.8470
  esticada     do cubo             1000000              19964.8           0.9982
```

A única linha fora de ordem é a penúltima de cada bloco, e ela também tem motivo: empurrado
meio cubo a partir de um ponto, o alvo sai da caixa dos dados em boa parte das vezes, e alvo
fora da caixa é o caso mais caro que existe.

**A nuvem agrupada, que eu escrevi como o melhor caso da poda, é o pior.**

Eu escrevi no javadoc que nuvem agrupada é o melhor caso, com o argumento de que o vizinho
está no mesmo grupo e a uma distância minúscula. Com alvo sorteado no cubo ela visita
**11.324** pontos contra **1.190** da uniforme: dez vezes pior, pelo motivo oposto. Quase
todo alvo cai no vazio **entre** os grupos, o vizinho mais próximo está longe e o raio nunca
aperta.

O argumento que eu dei só vale com alvo de dentro, e aí ela volta a 33,5 pontos. Não é a
nuvem que decide, é a relação entre o alvo e a nuvem.

**A regra de corte esperta inverte de sinal, e inverte pela origem do alvo.**

Eu escrevi que cortar o eixo de maior extensão é melhor do que rodar as dimensões por nível,
porque produz caixas mais cúbicas.

```
$ mvn exec:java -Dexec.args="regra"

   dimensoes nuvem        alvo         regra                  pontos visitados        vezes a ciclica
           2 esticada     do cubo      ciclica                           408.9                  1.000
           2 esticada     do cubo      maior extensao                   3175.0                  7.765
           2 esticada     de dentro    ciclica                            29.7                  1.000
           2 esticada     de dentro    maior extensao                     16.3                  0.550
           8 esticada     de dentro    ciclica                           186.9                  1.000
           8 esticada     de dentro    maior extensao                     30.8                  0.165
```

Na nuvem esticada com alvo de dentro ela ganha de **seis vezes**. Na mesma nuvem com alvo do
cubo ela perde de **quase oito**.

O motivo da perda: ali os alvos caem quase todos fora da nuvem no eixo estreito, e cortar o
eixo estreito não separa nada útil porque os dois lados ficam igualmente longe. A regra
cíclica, que corta o eixo largo em metade dos níveis aconteça o que acontecer, continua
partindo a distância ao meio; a regra esperta para de cortar o eixo largo assim que as caixas
ficam quadradas.

Na nuvem uniforme as duas empatam em tudo, com qualquer dimensão e qualquer alvo.

**Em dimensão 16, a ideia ingênua ganha da árvore.**

A projeção num eixo só ordena os pontos pela primeira coordenada e abre a partir do alvo até
a diferença nesse eixo passar do melhor raio. É correta, porque a diferença num eixo é um
limite inferior legítimo da distância, e é a primeira ideia que qualquer um tem.

Ela perde da árvore em dimensão 2 (139,6 contra 17,4), em 4 e em 8. Em dimensão 16 ela
**ganha**: 17.732 contra 19.751. Nenhuma das duas presta ali, mas a que não tem árvore
nenhuma presta um pouco mais, porque não paga os 4.095 nós nem as comparações de plano.

**A coluna das podas não pode ser lida como "mais é melhor".**

Ela sobe de 11,7 em dimensão 2 até 176,1 em dimensão 10 e **desce** para 11,9 em dimensão 16.
Os dois extremos têm quase o mesmo número e significam o oposto: em dimensão 2 sobra pouco
para podar porque quase tudo já morreu cedo, e em dimensão 16 não dá para podar nada.

**E uma medida só não resolve: a folha de um ponto ganha na coluna errada.**

```
$ mvn exec:java -Dexec.args="folha"

   dimensoes    folha     pontos visitados      nos abertos    soma dos dois        nos
           2        1                  5.1             25.7             30.8      39999
           2        4                  7.8             19.1             26.9      15423
           2       16                 17.4             14.3             31.7       4095
           2      256                182.0              8.4            190.4        255
           8        1                622.8           1594.9           2217.7      39999
           8       16               1190.7            319.0           1509.7       4095
           8      256               2823.0             52.1           2875.0        255
```

Folha de um ponto visita menos pontos que qualquer outra, em qualquer dimensão. E é a pior
escolha mesmo assim, porque abre cinco vezes mais nós. A soma tem fundo na folha 4 em
dimensão 2 e na folha 16 em dimensão 8.

## O que esta árvore não faz

Não insere nem apaga: ela é montada de uma vez sobre todos os pontos, cortando na mediana. É
por isso que não existe nuvem ruim para a **altura** (são 12 níveis para vinte mil pontos, em
qualquer nuvem e em qualquer dimensão) e é por isso que toda pergunta deste repositório é
sobre a **poda**, e não sobre o formato.

## Licença

MIT.
