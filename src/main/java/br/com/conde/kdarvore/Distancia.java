package br.com.conde.kdarvore;

/**
 * A DISTANCIA, ao quadrado e em inteiros.
 *
 * <p>Duas escolhas aqui, e as duas sao para que o juiz possa ser exato.
 *
 * <p>AO QUADRADO: a raiz quadrada nao muda a ordem de nada, e e a ordem que toda busca por
 * vizinho usa. Tirar a raiz so serviria para imprimir um numero bonito, e custaria perder a
 * exatidao.
 *
 * <p>EM INTEIROS: com coordenadas inteiras, a distancia ao quadrado e exata num long, e
 * entao "estes dois pontos empatam" e um fato e nao uma tolerancia. Com ponto flutuante,
 * dois pontos a mesma distancia do alvo poderiam dar numeros diferentes por um bit, e o juiz
 * acusaria divergencia onde nao ha nenhuma. Num repositorio cujo juiz confere resposta por
 * resposta, isso seria ruido permanente.
 */
public final class Distancia {

    private Distancia() {
    }

    /**
     * O teto de uma coordenada.
     *
     * <p>Com um milhao, a maior diferenca ao quadrado e 10^12, e em 16 dimensoes a soma
     * chega a 1,6 x 10^13. Um long guarda ate 9,2 x 10^18, entao sobra folga de cinco
     * ordens de grandeza e nenhuma conta deste repositorio transborda.
     */
    public static final int MAIOR_COORDENADA = 1_000_000;

    public static long aoQuadrado(int[] a, int[] b) {
        long soma = 0;

        for (int i = 0; i < a.length; i++) {
            long diferenca = (long) a[i] - b[i];

            soma += diferenca * diferenca;
        }

        return soma;
    }

    /** A distancia ao quadrado de um ponto ate um plano perpendicular a um dos eixos. */
    public static long ateOPlano(int[] ponto, int dimensao, int corte) {
        long diferenca = (long) ponto[dimensao] - corte;

        return diferenca * diferenca;
    }
}
