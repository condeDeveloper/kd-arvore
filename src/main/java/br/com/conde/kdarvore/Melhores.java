package br.com.conde.kdarvore;

import java.util.Arrays;

/**
 * OS K MELHORES ATE AGORA: um amontoado de maximo com capacidade fixa.
 *
 * <p>De maximo e nao de minimo, o que parece ao contrario e nao e. O que a busca pergunta a
 * cada ponto novo e "voce e melhor que o PIOR dos que eu ja tenho?", e e o pior que precisa
 * estar na mao. O melhor so interessa no fim.
 *
 * <p>E e esse pior que vira o raio de poda. Enquanto a lista nao tem k pontos, o raio e
 * infinito e nenhuma subarvore pode ser descartada: por isso uma consulta de k vizinhos com
 * k grande comeca cega e so passa a podar depois de encher.
 */
public final class Melhores {

    private final long[] distancias;
    private final int[] indices;
    private final int capacidade;

    private int quantos;

    public Melhores(int capacidade) {
        if (capacidade < 1) throw new IllegalArgumentException("pelo menos um vizinho");

        this.capacidade = capacidade;
        this.distancias = new long[capacidade];
        this.indices = new int[capacidade];
    }

    public int quantos() {
        return quantos;
    }

    public boolean cheio() {
        return quantos == capacidade;
    }

    /**
     * O raio de poda de hoje.
     *
     * <p>Infinito enquanto a lista nao encheu, porque ate la todo ponto novo entra e
     * nenhuma subarvore pode ser descartada sem risco.
     */
    public long pior() {
        return cheio() ? distancias[0] : Long.MAX_VALUE;
    }

    public void oferecer(long distancia, int indice) {
        if (!cheio()) {
            distancias[quantos] = distancia;
            indices[quantos] = indice;
            quantos++;
            subir(quantos - 1);
            return;
        }

        if (piorQue(distancia, indice, distancias[0], indices[0])) return;

        distancias[0] = distancia;
        indices[0] = indice;

        descer(0);
    }

    /**
     * A ordem e LEXICOGRAFICA em distancia e depois indice, e nao so em distancia.
     *
     * <p>Com coordenadas inteiras, empate de distancia acontece de verdade. Se o criterio
     * fosse so a distancia, "os k melhores" seria um conjunto ambiguo: a forca bruta, que
     * percorre por indice, ficaria com o primeiro empatado, e a arvore, que percorre por
     * folha, ficaria com outro. As duas estariam certas e o juiz acusaria divergencia.
     *
     * <p>Com o indice no criterio, o conjunto e unico e qualquer implementacao correta chega
     * ao mesmo. O preco e uma linha na poda: ela precisa descartar so o que e ESTRITAMENTE
     * pior, porque um ponto a distancia exatamente igual ainda pode ganhar pelo indice.
     */
    private static boolean piorQue(long distanciaA, int indiceA, long distanciaB, int indiceB) {
        if (distanciaA != distanciaB) return distanciaA > distanciaB;

        return indiceA >= indiceB;
    }

    /** Os indices guardados, do mais proximo ao mais distante. */
    public int[] emOrdem() {
        Integer[] posicoes = new Integer[quantos];

        for (int i = 0; i < quantos; i++) posicoes[i] = i;

        Arrays.sort(posicoes, (a, b) -> {
            int porDistancia = Long.compare(distancias[a], distancias[b]);

            /*
             * O DESEMPATE PELO INDICE NAO E ENFEITE. Com pontos sorteados em grade inteira,
             * empate de distancia acontece, e sem um criterio estavel duas estruturas
             * corretas devolveriam listas diferentes e o juiz acusaria divergencia onde nao
             * ha nenhuma.
             */
            return porDistancia != 0 ? porDistancia : Integer.compare(indices[a], indices[b]);
        });

        int[] saida = new int[quantos];

        for (int i = 0; i < quantos; i++) saida[i] = indices[posicoes[i]];

        return saida;
    }

    /** As distancias guardadas, em ordem crescente. */
    public long[] distanciasEmOrdem() {
        long[] copia = Arrays.copyOf(distancias, quantos);

        Arrays.sort(copia);

        return copia;
    }

    private void subir(int i) {
        while (i > 0) {
            int pai = (i - 1) / 2;

            if (!maiorQue(i, pai)) break;

            trocar(pai, i);
            i = pai;
        }
    }

    private void descer(int i) {
        while (true) {
            int maior = i;
            int esquerda = 2 * i + 1;
            int direita = 2 * i + 2;

            if (esquerda < quantos && maiorQue(esquerda, maior)) maior = esquerda;
            if (direita < quantos && maiorQue(direita, maior)) maior = direita;

            if (maior == i) return;

            trocar(maior, i);
            i = maior;
        }
    }

    /** O a esta mais longe que o b na ordem lexicografica de distancia e indice. */
    private boolean maiorQue(int a, int b) {
        return piorQue(distancias[a], indices[a], distancias[b], indices[b]);
    }

    private void trocar(int a, int b) {
        long guardaDistancia = distancias[a];
        int guardaIndice = indices[a];

        distancias[a] = distancias[b];
        indices[a] = indices[b];
        distancias[b] = guardaDistancia;
        indices[b] = guardaIndice;
    }
}
