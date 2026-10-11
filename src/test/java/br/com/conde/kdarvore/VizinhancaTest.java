package br.com.conde.kdarvore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VizinhancaTest {

    // ───────────────────── o juiz contra todas as versoes ─────────────────────

    @Test
    @DisplayName("toda versao, toda nuvem e toda dimensao batem com a forca bruta")
    void todasBatemComOJuiz() {
        List<String> falhas = new ArrayList<>();
        int conferidas = 0;

        for (int dimensoes : new int[] {1, 2, 3, 5, 8}) {
            int[][] alvos = Nuvens.alvos(30, dimensoes, 7);

            for (var nuvem : Nuvens.todas(800, dimensoes, 3).entrySet())
                for (var versao : Juiz.asVersoes().entrySet()) {
                    Vizinhanca vizinhanca =
                            versao.getValue().apply(nuvem.getValue(), new Contador());

                    Juiz.Veredito veredito = Juiz.julgar(vizinhanca, nuvem.getValue(), alvos, 4);

                    conferidas++;

                    if (!veredito.passou())
                        falhas.add(versao.getKey() + " em " + nuvem.getKey() + " com "
                                + dimensoes + " dimensoes: " + veredito.primeiraFalha());
                }
        }

        assertThat(conferidas).isGreaterThan(50);
        assertThat(falhas).isEmpty();
    }

    /**
     * ALVOS DE DENTRO DA NUVEM SAO UM CASO SEPARADO, e nao um caso mais facil.
     *
     * <p>Com o alvo em cima de um ponto, a distancia ao vizinho pode ser zero, e o raio de
     * poda vira zero logo no comeco. Uma poda escrita com maior ou igual em vez de maior
     * descartaria tudo dali em diante e ainda assim responderia certo em quase toda
     * consulta.
     */
    @Test
    @DisplayName("com o alvo em cima de um ponto, a resposta continua batendo")
    void alvoEmCimaDeUmPonto() {
        for (int dimensoes : new int[] {2, 5, 8}) {
            int[][] pontos = Nuvens.uniforme(1_000, dimensoes, 11);
            int[][] alvos = Nuvens.alvosDeDentro(pontos, 50, 0, 13);

            for (var versao : Juiz.asVersoes().entrySet()) {
                Vizinhanca vizinhanca = versao.getValue().apply(pontos, new Contador());
                Juiz.Veredito veredito = Juiz.julgar(vizinhanca, pontos, alvos, 4);

                assertThat(veredito.divergencias())
                        .as(versao.getKey() + " com " + dimensoes + " dimensoes: "
                                + veredito.primeiraFalha())
                        .isZero();
            }
        }
    }

    /**
     * PONTOS REPETIDOS SAO O CASO QUE QUEBRA SELECAO E DESEMPATE AO MESMO TEMPO.
     *
     * <p>Com muitas coordenadas iguais, a particao da mediana pode nao separar nada, e
     * varios pontos empatam em distancia exata. As duas coisas juntas sao o pior caso dos
     * dois mecanismos que este repositorio escreveu a mao.
     */
    @Test
    @DisplayName("nuvem cheia de pontos repetidos nao quebra nem a montagem nem o desempate")
    void pontosRepetidos() {
        int[][] pontos = new int[600][3];
        Nuvens.Sorteio sorteio = new Nuvens.Sorteio(5);

        for (int i = 0; i < pontos.length; i++)
            for (int d = 0; d < 3; d++) pontos[i][d] = sorteio.ate(4);

        int[][] alvos = new int[20][3];

        for (int i = 0; i < alvos.length; i++)
            for (int d = 0; d < 3; d++) alvos[i][d] = sorteio.ate(6) - 1;

        for (int folha : new int[] {1, 4, 32}) {
            KdArvore arvore = new KdArvore(pontos, KdArvore.Regra.MAIOR_EXTENSAO, true, folha,
                    new Contador());

            arvore.conferirInvariantes();

            Juiz.Veredito veredito = Juiz.julgar(arvore, pontos, alvos, 7);

            assertThat(veredito.divergencias())
                    .as("folha " + folha + ": " + veredito.primeiraFalha())
                    .isZero();
        }
    }

    @Test
    @DisplayName("todos os pontos no mesmo lugar nao travam a montagem")
    void todosNoMesmoLugar() {
        int[][] pontos = new int[500][4];

        for (int[] ponto : pontos) java.util.Arrays.fill(ponto, 77);

        KdArvore arvore = new KdArvore(pontos, KdArvore.Regra.MAIOR_EXTENSAO, true, 1,
                new Contador());

        arvore.conferirInvariantes();

        assertThat(arvore.maisProximo(new int[] {77, 77, 77, 77})).isZero();
        assertThat(arvore.maisProximos(new int[] {0, 0, 0, 0}, 3)).containsExactly(0, 1, 2);
    }

    // ───────────────────── as invariantes ─────────────────────

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 7, 16})
    @DisplayName("as invariantes da arvore valem em qualquer dimensao e qualquer folha")
    void asInvariantesValem(int dimensoes) {
        for (var nuvem : Nuvens.todas(1_000, dimensoes, 9).values())
            for (int folha : new int[] {1, 3, 16, 128})
                for (KdArvore.Regra regra : KdArvore.Regra.values())
                    new KdArvore(nuvem, regra, true, folha, new Contador())
                            .conferirInvariantes();
    }

    @Test
    @DisplayName("a altura e logaritmica, porque o corte e sempre na mediana")
    void aAlturaELogaritmica() {
        for (var nuvem : Nuvens.todas(20_000, 4, 2).entrySet()) {
            KdArvore arvore = KdArvore.padrao(nuvem.getValue(), new Contador());

            assertThat(arvore.altura())
                    .as("nuvem " + nuvem.getKey() + ": nao existe nuvem ruim para a altura")
                    .isEqualTo(12);
        }
    }

    @Test
    @DisplayName("folha menor que um nao e folha")
    void folhaMinima() {
        assertThatThrownBy(() -> new KdArvore(Nuvens.uniforme(10, 2, 1),
                KdArvore.Regra.CICLICA, true, 0, new Contador()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ───────────────────── a poda ─────────────────────

    /**
     * A poda e o algoritmo inteiro, entao ela precisa de um teste que afirme as duas coisas:
     * que ela corta muito E que ela nao muda nenhuma resposta.
     */
    @Test
    @DisplayName("podar visita cem vezes menos pontos e devolve exatamente a mesma resposta")
    void podarNaoMudaResposta() {
        int[][] pontos = Nuvens.uniforme(5_000, 3, 4);
        int[][] alvos = Nuvens.alvos(100, 3, 6);

        Contador comPoda = new Contador();
        Contador semPoda = new Contador();

        KdArvore podada = new KdArvore(pontos, KdArvore.Regra.MAIOR_EXTENSAO, true, 16, comPoda);
        KdArvore inteira = new KdArvore(pontos, KdArvore.Regra.MAIOR_EXTENSAO, false, 16, semPoda);

        for (int[] alvo : alvos)
            assertThat(podada.maisProximos(alvo, 5))
                    .containsExactly(inteira.maisProximos(alvo, 5));

        assertThat(semPoda.pontosPorConsulta())
                .as("sem poda ela visita todos os pontos, igual a forca bruta")
                .isEqualTo(5_000.0);

        assertThat(comPoda.pontosPorConsulta()).isLessThan(100.0);
    }

    // ───────────────────── os k melhores ─────────────────────

    @Test
    @DisplayName("o amontoado guarda os k menores e o pior fica no topo")
    void oAmontoadoGuardaOsMenores() {
        Melhores melhores = new Melhores(3);

        assertThat(melhores.pior()).isEqualTo(Long.MAX_VALUE);

        melhores.oferecer(50, 0);
        melhores.oferecer(10, 1);

        assertThat(melhores.pior())
                .as("com a lista pela metade, nada pode ser descartado")
                .isEqualTo(Long.MAX_VALUE);

        melhores.oferecer(30, 2);

        assertThat(melhores.cheio()).isTrue();
        assertThat(melhores.pior()).isEqualTo(50);

        melhores.oferecer(20, 3);

        assertThat(melhores.pior()).isEqualTo(30);
        assertThat(melhores.emOrdem()).containsExactly(1, 3, 2);
        assertThat(melhores.distanciasEmOrdem()).containsExactly(10, 20, 30);

        melhores.oferecer(99, 4);

        assertThat(melhores.emOrdem()).containsExactly(1, 3, 2);
    }

    /**
     * O DESEMPATE E O QUE DEIXA O JUIZ POSSIVEL.
     *
     * <p>Com coordenadas inteiras, empate de distancia acontece. Sem um criterio estavel, a
     * forca bruta e a arvore devolveriam indices diferentes, as duas certas, e nao haveria
     * como julgar nada.
     */
    @Test
    @DisplayName("empate de distancia e resolvido sempre pelo menor indice")
    void oEmpateVaiPeloMenorIndice() {
        Melhores melhores = new Melhores(2);

        melhores.oferecer(10, 9);
        melhores.oferecer(10, 4);

        assertThat(melhores.emOrdem()).containsExactly(4, 9);

        melhores.oferecer(10, 1);

        assertThat(melhores.emOrdem())
                .as("o indice 1 empata com os dois e entra pelo criterio")
                .containsExactly(1, 4);

        melhores.oferecer(10, 7);

        assertThat(melhores.emOrdem())
                .as("o indice 7 empata e perde, porque e maior")
                .containsExactly(1, 4);
    }

    @Test
    @DisplayName("os quatro pontos de um quadrado empatam, e todos concordam em qual escolher")
    void quatroPontosEmpatados() {
        int[][] pontos = {{0, 0}, {10, 0}, {0, 10}, {10, 10}};
        int[] centro = {5, 5};

        for (var versao : Juiz.asVersoes().entrySet()) {
            Vizinhanca vizinhanca = versao.getValue().apply(pontos, new Contador());

            assertThat(vizinhanca.maisProximo(centro)).as(versao.getKey()).isZero();
            assertThat(vizinhanca.maisProximos(centro, 4)).as(versao.getKey())
                    .containsExactly(0, 1, 2, 3);
        }
    }

    @Test
    @DisplayName("pedir zero vizinhos nao faz sentido")
    void zeroVizinhos() {
        assertThatThrownBy(() -> new Melhores(0)).isInstanceOf(IllegalArgumentException.class);
    }

    // ───────────────────── o raio ─────────────────────

    @Test
    @DisplayName("o raio zero traz so quem esta exatamente no lugar")
    void raioZero() {
        int[][] pontos = {{1, 1}, {1, 1}, {2, 2}};

        for (var versao : Juiz.asVersoes().entrySet()) {
            Vizinhanca vizinhanca = versao.getValue().apply(pontos, new Contador());

            assertThat(vizinhanca.numRaio(new int[] {1, 1}, 0)).as(versao.getKey())
                    .containsExactly(0, 1);
        }
    }

    @Test
    @DisplayName("um raio que cobre tudo traz todos os pontos")
    void raioQueCobreTudo() {
        int[][] pontos = Nuvens.uniforme(300, 3, 8);

        for (var versao : Juiz.asVersoes().entrySet()) {
            Vizinhanca vizinhanca = versao.getValue().apply(pontos, new Contador());

            assertThat(vizinhanca.numRaio(new int[] {0, 0, 0}, Long.MAX_VALUE / 4))
                    .as(versao.getKey()).hasSize(300);
        }
    }

    @Test
    @DisplayName("a vizinhanca vazia responde menos um e nada")
    void vizinhancaVazia() {
        int[][] nenhum = new int[0][2];

        for (var versao : Juiz.asVersoes().entrySet()) {
            Vizinhanca vizinhanca = versao.getValue().apply(nenhum, new Contador());

            assertThat(vizinhanca.maisProximo(new int[] {1, 1})).as(versao.getKey()).isEqualTo(-1);
            assertThat(vizinhanca.maisProximos(new int[] {1, 1}, 3)).as(versao.getKey()).isEmpty();
            assertThat(vizinhanca.numRaio(new int[] {1, 1}, 100)).as(versao.getKey()).isEmpty();
        }
    }

    // ───────────────────── a distancia ─────────────────────

    @Test
    @DisplayName("a distancia ao quadrado cabe num long com folga, em 16 dimensoes")
    void aDistanciaNaoTransborda() {
        int[] cantoA = new int[16];
        int[] cantoB = new int[16];

        java.util.Arrays.fill(cantoB, Distancia.MAIOR_COORDENADA);

        long maior = Distancia.aoQuadrado(cantoA, cantoB);

        assertThat(maior).isEqualTo(16L * Distancia.MAIOR_COORDENADA * Distancia.MAIOR_COORDENADA);
        assertThat(maior).isEqualTo(16_000_000_000_000L);
        assertThat(maior).isLessThan(Long.MAX_VALUE / 500_000);
    }

    // ───────────────────── o sorteio da casa ─────────────────────

    /**
     * O TESTE DO DESLOCAMENTO DE 32.
     *
     * <p>Sem o {@code >>> 32}, o gerador entregaria os bits baixos, cujo ultimo bit alterna
     * zero e um para sempre. Numa nuvem de pontos isso nao seria um detalhe estatistico: as
     * coordenadas cairiam numa rede regular, e numa rede regular os vizinhos ficam a
     * distancia constante. A poda estaria medindo a rede e nao a dimensao.
     */
    @Test
    @DisplayName("o deslocamento de 32 e o que faz o sorteio sortear")
    void oDeslocamentoDeTrintaEDois() {
        Nuvens.Sorteio sorteio = new Nuvens.Sorteio(1);
        int[] quantos = new int[4];

        for (int i = 0; i < 40_000; i++) quantos[sorteio.ate(4)]++;

        for (int resto = 0; resto < 4; resto++)
            assertThat(quantos[resto]).as("resto " + resto).isBetween(9_600, 10_400);

        long estado = 1;
        int alternancias = 0;
        int anterior = -1;

        for (int i = 0; i < 1_000; i++) {
            estado = estado * 6364136223846793005L + 1442695040888963407L;

            int baixo = (int) (estado & 1);

            if (anterior >= 0 && baixo != anterior) alternancias++;

            anterior = baixo;
        }

        assertThat(alternancias)
                .as("o ultimo bit do estado cru alterna em TODOS os passos")
                .isEqualTo(999);
    }

    @Test
    @DisplayName("a nuvem esticada e esticada mesmo, e a agrupada e agrupada")
    void asNuvensSaoOQueDizem() {
        int[][] esticada = Nuvens.esticada(2_000, 4, 1);

        assertThat(extensao(esticada, 0)).isGreaterThan(900_000);
        assertThat(extensao(esticada, 1)).isLessThan(11_000);

        int[][] grupos = Nuvens.emGrupos(2_000, 4, 10, 1);
        int[][] uniforme = Nuvens.uniforme(2_000, 4, 1);

        ForcaBruta daNuvem = new ForcaBruta(grupos, new Contador());
        ForcaBruta doUniforme = new ForcaBruta(uniforme, new Contador());

        assertThat(daNuvem.todasAsDistancias(grupos[0])[1])
                .as("num grupo o vizinho esta muito mais perto do que no uniforme")
                .isLessThan(doUniforme.todasAsDistancias(uniforme[0])[1]);
    }

    private static long extensao(int[][] pontos, int dimensao) {
        int menor = Integer.MAX_VALUE;
        int maior = Integer.MIN_VALUE;

        for (int[] ponto : pontos) {
            menor = Math.min(menor, ponto[dimensao]);
            maior = Math.max(maior, ponto[dimensao]);
        }

        return (long) maior - menor;
    }
}
