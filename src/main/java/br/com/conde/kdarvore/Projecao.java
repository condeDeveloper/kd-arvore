package br.com.conde.kdarvore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A PROJECAO NUM EIXO SO: ordenar os pontos pela primeira coordenada e abrir a partir do
 * alvo, parando quando a diferenca nesse eixo ja passar do melhor raio.
 *
 * <p>Ela esta aqui porque e a primeira ideia que qualquer um tem, ela e CORRETA, e ela e
 * ruim. Essas tres coisas juntas sao raras e valem uma coluna.
 *
 * <p>Correta porque a diferenca num unico eixo e um limite inferior legitimo da distancia:
 * se dois pontos ja estao a mil de diferenca na primeira coordenada, eles estao a pelo menos
 * mil de distancia, tenham o que tiverem nas outras.
 *
 * <p>Ruim porque esse limite ignora d-1 dimensoes. Em dimensao alta, o vizinho mais proximo
 * esta longe em distancia total e perto em cada eixo isolado, e a faixa que precisa ser
 * aberta engole a nuvem inteira.
 */
public final class Projecao implements Vizinhanca {

    private final int[][] pontos;
    private final int[] ordem;
    private final int[] coordenadas;
    private final Contador contador;

    public Projecao(int[][] pontos, Contador contador) {
        this.pontos = pontos;
        this.contador = contador;
        this.ordem = new int[pontos.length];
        this.coordenadas = new int[pontos.length];

        Integer[] posicoes = new Integer[pontos.length];

        for (int i = 0; i < pontos.length; i++) posicoes[i] = i;

        Arrays.sort(posicoes, (a, b) -> Integer.compare(pontos[a][0], pontos[b][0]));

        for (int i = 0; i < pontos.length; i++) {
            ordem[i] = posicoes[i];
            coordenadas[i] = pontos[posicoes[i]][0];
        }
    }

    @Override
    public String nome() {
        return "projecao num eixo";
    }

    /** A primeira posicao cuja coordenada ja e maior ou igual ao valor. */
    private int posicaoDe(int valor) {
        int baixo = 0;
        int alto = coordenadas.length;

        while (baixo < alto) {
            int meio = (baixo + alto) >>> 1;

            if (coordenadas[meio] < valor) baixo = meio + 1;
            else alto = meio;
        }

        return baixo;
    }

    @Override
    public int maisProximo(int[] alvo) {
        int[] um = maisProximos(alvo, 1);

        return um.length == 0 ? -1 : um[0];
    }

    @Override
    public int[] maisProximos(int[] alvo, int k) {
        contador.consulta();

        Melhores melhores = new Melhores(k);

        if (pontos.length == 0) return melhores.emOrdem();

        int posicao = posicaoDe(alvo[0]);
        int esquerda = posicao - 1;
        int direita = posicao;

        while (true) {
            long peloEixoEsquerda = esquerda >= 0 ? noEixo(alvo, esquerda) : Long.MAX_VALUE;
            long peloEixoDireita = direita < coordenadas.length
                    ? noEixo(alvo, direita) : Long.MAX_VALUE;

            long menor = Math.min(peloEixoEsquerda, peloEixoDireita);

            if (menor == Long.MAX_VALUE) break;

            if (menor > melhores.pior()) {
                contador.poda();
                break;
            }

            int escolhido = peloEixoEsquerda <= peloEixoDireita ? esquerda-- : direita++;
            int indice = ordem[escolhido];

            contador.visitaPonto();
            melhores.oferecer(Distancia.aoQuadrado(pontos[indice], alvo), indice);
        }

        return melhores.emOrdem();
    }

    @Override
    public int[] numRaio(int[] alvo, long raioAoQuadrado) {
        contador.consulta();

        List<Integer> dentro = new ArrayList<>();

        if (pontos.length == 0) return new int[0];

        int posicao = posicaoDe(alvo[0]);

        for (int i = posicao; i < coordenadas.length; i++) {
            if (noEixo(alvo, i) > raioAoQuadrado) {
                contador.poda();
                break;
            }

            contador.visitaPonto();

            if (Distancia.aoQuadrado(pontos[ordem[i]], alvo) <= raioAoQuadrado)
                dentro.add(ordem[i]);
        }

        for (int i = posicao - 1; i >= 0; i--) {
            if (noEixo(alvo, i) > raioAoQuadrado) {
                contador.poda();
                break;
            }

            contador.visitaPonto();

            if (Distancia.aoQuadrado(pontos[ordem[i]], alvo) <= raioAoQuadrado)
                dentro.add(ordem[i]);
        }

        int[] saida = new int[dentro.size()];

        for (int i = 0; i < saida.length; i++) saida[i] = dentro.get(i);

        Arrays.sort(saida);

        return saida;
    }

    private long noEixo(int[] alvo, int posicao) {
        long diferenca = (long) coordenadas[posicao] - alvo[0];

        return diferenca * diferenca;
    }

    @Override
    public int nos() {
        return 1;
    }

    @Override
    public int altura() {
        return 1;
    }
}
