package br.com.conde.kdarvore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AS NUVENS DE PONTOS. Elas nao sao decoracao: a nuvem decide a resposta.
 *
 * <p>Uma arvore k-d montada por mediana tem a mesma altura em qualquer nuvem, entao e
 * tentador achar que a nuvem nao importa. Importa, e importa na poda: o que decide quantas
 * subarvores morrem sem ser abertas e o formato das caixas e a distancia tipica entre
 * vizinhos, e as duas coisas vem da nuvem.
 */
public final class Nuvens {

    private Nuvens() {
    }

    /**
     * O SORTEIO DA CASA: um gerador congruente linear escrito aqui, com constantes fixas.
     *
     * <p>Nao e {@link java.util.Random} de proposito. Uma medida que depende de uma
     * biblioteca nao e reproduzivel: a implementacao pode mudar entre versoes da maquina
     * virtual e as tabelas do README deixariam de bater sem que nada neste repositorio
     * tivesse mudado.
     */
    public static final class Sorteio {

        private long estado;

        public Sorteio(long semente) {
            this.estado = semente;
        }

        /**
         * O proximo inteiro.
         *
         * <p>O DESLOCAMENTO DE 32 CARREGA O METODO. Os bits BAIXOS de um gerador congruente
         * linear de modulo potencia de dois sao pessimos: o ultimo bit alterna zero e um
         * para sempre, o penultimo tem periodo quatro, e assim por diante. Devolver
         * {@code (int) estado} entregaria esses bits baixos.
         *
         * <p>Numa nuvem de pontos isso nao seria um detalhe estatistico. Coordenadas
         * sorteadas por resto de uma potencia de dois com bits baixos ruins cairiam numa
         * REDE REGULAR, e uma rede regular tem vizinhos a distancia constante: a poda da
         * arvore k-d mediria a rede e nao a dimensao. Ha um teste so para isto.
         */
        public int proximo() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;
            return (int) (estado >>> 32);
        }

        public int ate(int limite) {
            return Math.floorMod(proximo(), limite);
        }
    }

    /** Pontos espalhados por igual no cubo inteiro. */
    public static int[][] uniforme(int quantos, int dimensoes, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        int[][] pontos = new int[quantos][dimensoes];

        for (int i = 0; i < quantos; i++)
            for (int d = 0; d < dimensoes; d++)
                pontos[i][d] = sorteio.ate(Distancia.MAIOR_COORDENADA);

        return pontos;
    }

    /**
     * Pontos em grupos apertados, que e como dado de verdade costuma vir.
     *
     * <p>Eu escrevi aqui que nuvem agrupada e o MELHOR caso da poda, com o argumento de que
     * o vizinho esta no mesmo grupo e a uma distancia minuscula. Com alvos sorteados no cubo
     * inteiro ela e o PIOR caso, dez vezes pior que a uniforme, e pelo motivo oposto: quase
     * todo alvo cai no vazio ENTRE os grupos, o vizinho mais proximo esta longe e o raio
     * nunca aperta.
     *
     * <p>O argumento so vale com alvos de dentro da nuvem, e e isso que
     * {@link #alvosDeDentro} mede. A tabela do alvo tem as duas colunas.
     */
    public static int[][] emGrupos(int quantos, int dimensoes, int grupos, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        int[][] centros = new int[grupos][dimensoes];

        for (int g = 0; g < grupos; g++)
            for (int d = 0; d < dimensoes; d++)
                centros[g][d] = sorteio.ate(Distancia.MAIOR_COORDENADA);

        int raio = Distancia.MAIOR_COORDENADA / 200;
        int[][] pontos = new int[quantos][dimensoes];

        for (int i = 0; i < quantos; i++) {
            int grupo = sorteio.ate(grupos);

            for (int d = 0; d < dimensoes; d++)
                pontos[i][d] = centros[grupo][d] + sorteio.ate(2 * raio + 1) - raio;
        }

        return pontos;
    }

    /**
     * Pontos esticados: a primeira dimensao ocupa o cubo inteiro e as outras ocupam um
     * centesimo dele.
     *
     * <p>Esta e a nuvem que separa as duas regras de corte, e separa NOS DOIS SENTIDOS. Eu
     * escrevi aqui que a regra ciclica perderia feio por gastar metade dos cortes em eixos
     * onde quase nao ha o que separar. Com alvo tirado de dentro da nuvem e isso mesmo: em
     * dimensao 8 a maior extensao visita 30,8 pontos contra 186,9 da ciclica.
     *
     * <p>Com alvo sorteado no cubo, em dimensao 2, acontece o contrario e por quase OITO
     * VEZES: 3.175 contra 408,9. Ali os alvos caem quase todos fora da nuvem no eixo
     * estreito, cortar esse eixo nao separa nada util, e quem continua partindo o eixo largo
     * por teimosia leva vantagem.
     *
     * <p>E ela tambem nao e exotica: dados com unidades diferentes em cada coluna sao assim
     * por padrao, e normalizar antes de indexar e justamente o conselho que existe por
     * causa deste efeito.
     */
    public static int[][] esticada(int quantos, int dimensoes, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        int[][] pontos = new int[quantos][dimensoes];
        int estreito = Math.max(2, Distancia.MAIOR_COORDENADA / 100);

        for (int i = 0; i < quantos; i++) {
            pontos[i][0] = sorteio.ate(Distancia.MAIOR_COORDENADA);

            for (int d = 1; d < dimensoes; d++) pontos[i][d] = sorteio.ate(estreito);
        }

        return pontos;
    }

    /** Alvos sorteados no cubo inteiro, para perguntar. */
    public static int[][] alvos(int quantos, int dimensoes, long semente) {
        return uniforme(quantos, dimensoes, semente);
    }

    /**
     * Alvos tirados de DENTRO da propria nuvem, com um empurrao pequeno.
     *
     * <p>Esta e a diferenca que eu nao tinha previsto e que as tabelas me mostraram. Com
     * alvos sorteados no cubo inteiro, uma nuvem agrupada e o PIOR caso da poda e nao o
     * melhor: quase todo alvo cai no vazio entre os grupos, o vizinho mais proximo esta
     * longe, o raio fica frouxo e nada e descartado.
     *
     * <p>O que decide nao e a nuvem: e a relacao entre o alvo e a nuvem. E consulta de
     * verdade quase sempre vem de dentro, porque quem pergunta "quem se parece com isto"
     * ja tem um "isto" que veio do mesmo lugar que os dados.
     */
    public static int[][] alvosDeDentro(int[][] nuvem, int quantos, long semente) {
        return alvosDeDentro(nuvem, quantos, Distancia.MAIOR_COORDENADA / 2_000, semente);
    }

    /**
     * O mesmo, com o empurrao escolhido.
     *
     * <p>O empurrao e o que mede a distancia entre os dois extremos. Empurrao pequeno e
     * "ache o vizinho deste item que eu ja tenho"; empurrao do tamanho do cubo e um alvo
     * sorteado, que nao se parece com nada. A tabela do alvo percorre o caminho entre os
     * dois.
     */
    public static int[][] alvosDeDentro(int[][] nuvem, int quantos, int empurrao, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        int dimensoes = nuvem[0].length;
        int[][] alvos = new int[quantos][dimensoes];

        for (int i = 0; i < quantos; i++) {
            int[] base = nuvem[sorteio.ate(nuvem.length)];

            if (empurrao < 1) {
                alvos[i] = base.clone();
                continue;
            }

            for (int d = 0; d < dimensoes; d++)
                alvos[i][d] = base[d] + sorteio.ate(2 * empurrao + 1) - empurrao;
        }

        return alvos;
    }

    public static Map<String, int[][]> todas(int quantos, int dimensoes, long semente) {
        Map<String, int[][]> nuvens = new LinkedHashMap<>();

        nuvens.put("uniforme", uniforme(quantos, dimensoes, semente));
        nuvens.put("em grupos", emGrupos(quantos, dimensoes, 40, semente + 1));
        nuvens.put("esticada", esticada(quantos, dimensoes, semente + 2));

        return nuvens;
    }
}
