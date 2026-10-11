package br.com.conde.kdarvore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros do README, afirmados.
 *
 * <p>Um README e texto: ele nao quebra quando deixa de ser verdade. Cada afirmacao das
 * tabelas esta repetida aqui para quebrar.
 */
class MedidasTest {

    /*
     * AS TABELAS SAO CALCULADAS UMA VEZ SO. Cada uma monta varias arvores de vinte mil
     * pontos; chamadas de dentro dos ajudantes, elas seriam remontadas a cada busca de
     * linha.
     */
    private static final List<Medidas.LinhaDimensao> DIMENSAO = Medidas.aDimensao();
    private static final List<Medidas.LinhaPoda> PODA = Medidas.aPoda();
    private static final List<Medidas.LinhaRegra> REGRA = Medidas.aRegra();
    private static final List<Medidas.LinhaAlvo> ALVO = Medidas.oAlvo();
    private static final List<Medidas.LinhaKvizinhos> KVIZINHOS = Medidas.osKvizinhos();
    private static final List<Medidas.LinhaFolha> FOLHA = Medidas.aFolha();

    // ───────────────────── 1. a correcao ─────────────────────

    @Test
    @DisplayName("nenhuma combinacao diverge da forca bruta")
    void aCorrecaoNaoDiverge() {
        List<Medidas.LinhaCorrecao> linhas = Medidas.aCorrecao();

        assertThat(linhas).hasSize(36);
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.divergencias())
                    .as(l.versao() + " em " + l.nuvem() + " com " + l.dimensoes() + " dimensoes")
                    .isZero();
            assertThat(l.consultas()).isEqualTo(120);
        });
    }

    // ───────────────────── 2. a dimensao ─────────────────────

    /**
     * A MALDICAO DA DIMENSIONALIDADE, medida.
     *
     * <p>Com alvo sorteado no cubo, a arvore k-d passa de 17,4 pontos visitados em dimensao
     * 2 para 19.751,8 em dimensao 16, que e 98,8% dos vinte mil. Ela deixa de ser uma
     * estrutura de busca e vira uma forca bruta com ponteiros.
     */
    @Test
    @DisplayName("com alvo do cubo, a arvore visita 98,8% dos pontos em dimensao 16")
    void aMaldicaoEmNumeros() {
        assertThat(dimensao(2).doCubo()).isCloseTo(17.4, within(0.1));
        assertThat(dimensao(4).doCubo()).isCloseTo(73.6, within(0.1));
        assertThat(dimensao(8).doCubo()).isCloseTo(1190.7, within(0.1));
        assertThat(dimensao(12).doCubo()).isCloseTo(13631.4, within(0.1));
        assertThat(dimensao(16).doCubo()).isCloseTo(19751.8, within(0.1));

        assertThat(dimensao(2).fracaoDoCubo()).isLessThan(0.001);
        assertThat(dimensao(16).fracaoDoCubo()).isGreaterThan(0.98);

        for (int i = 1; i < DIMENSAO.size(); i++)
            assertThat(DIMENSAO.get(i).doCubo())
                    .as("dimensao " + DIMENSAO.get(i).dimensoes() + " custa mais que a anterior")
                    .isGreaterThan(DIMENSAO.get(i - 1).doCubo());
    }

    /**
     * A SEGUNDA COLUNA, que e a que eu nao tinha pensado em medir.
     *
     * <p>Com o alvo tirado de dentro da propria nuvem, a maldicao simplesmente nao aparece
     * nestes tamanhos: em dimensao 16 a arvore continua visitando treze pontos.
     */
    @Test
    @DisplayName("com alvo de dentro da nuvem, a maldicao nao aparece de 2 a 16 dimensoes")
    void comAlvoDeDentroNaoHaMaldicao() {
        assertThat(DIMENSAO).allSatisfy(l ->
                assertThat(l.deDentro())
                        .as("dimensao " + l.dimensoes())
                        .isBetween(10.0, 35.0));

        assertThat(dimensao(16).deDentro()).isCloseTo(13.2, within(0.1));

        assertThat(dimensao(16).doCubo() / dimensao(16).deDentro())
                .as("em dimensao 16 a mesma arvore custa mil e quinhentas vezes mais ou menos "
                        + "conforme de onde vem o alvo")
                .isGreaterThan(1_400.0);
    }

    /**
     * A COLUNA DAS PODAS NAO PODE SER LIDA COMO "MAIS E MELHOR".
     *
     * <p>Ela sobe de 11,7 em dimensao 2 ate 176,1 em dimensao 10 e DESCE para 11,9 em
     * dimensao 16. Os dois extremos tem quase o mesmo numero e significam o oposto: em
     * dimensao 2 sobra pouco para podar porque quase tudo ja morreu cedo, e em dimensao 16
     * nao da para podar nada.
     */
    @Test
    @DisplayName("as podas por consulta sobem ate dimensao 10 e caem depois")
    void asPodasSaoNaoMonotonas() {
        assertThat(dimensao(2).podas()).isCloseTo(11.7, within(0.1));
        assertThat(dimensao(10).podas()).isCloseTo(176.1, within(0.1));
        assertThat(dimensao(16).podas()).isCloseTo(11.9, within(0.1));

        assertThat(dimensao(10).podas()).isGreaterThan(dimensao(2).podas());
        assertThat(dimensao(16).podas()).isLessThan(dimensao(10).podas());

        assertThat(dimensao(16).podas())
                .as("o extremo de baixo e o de cima quase empatam, e significam o oposto")
                .isCloseTo(dimensao(2).podas(), within(0.5));
    }

    @Test
    @DisplayName("a altura e o numero de nos nao mudam com a dimensao")
    void aFormaNaoMudaComADimensao() {
        assertThat(DIMENSAO).allSatisfy(l -> {
            assertThat(l.altura()).isEqualTo(12);
            assertThat(l.nos()).isEqualTo(4095);
        });
    }

    private static Medidas.LinhaDimensao dimensao(int dimensoes) {
        return DIMENSAO.stream().filter(l -> l.dimensoes() == dimensoes).findFirst().orElseThrow();
    }

    // ───────────────────── 3. a poda ─────────────────────

    @Test
    @DisplayName("sem poda a arvore visita exatamente os n pontos, igual a forca bruta")
    void semPodaNaoEconomizaNada() {
        for (int dimensoes : new int[] {2, 4, 8, 16}) {
            assertThat(poda(dimensoes, "kd sem poda").pontosVisitados())
                    .as("dimensao " + dimensoes)
                    .isEqualTo(Medidas.PONTOS);

            assertThat(poda(dimensoes, "forca bruta").pontosVisitados())
                    .isEqualTo(Medidas.PONTOS);
        }

        assertThat(poda(2, "kd com poda").pontosVisitados())
                .as("e com poda, na mesma arvore, mil vezes menos")
                .isLessThan(20.0);
    }

    /**
     * A INVERSAO QUE EU NAO TINHA PREVISTO NESTA TABELA.
     *
     * <p>A projecao num eixo e a ideia ingenua e perde da arvore em dimensao 2, 4 e 8. Em
     * dimensao 16 ela GANHA: 17.732 contra 19.751. Nenhuma das duas presta ali, mas a que
     * nao tem arvore nenhuma presta um pouco mais.
     */
    @Test
    @DisplayName("em dimensao 16 a projecao ingenua ganha da arvore k-d")
    void emDimensaoAltaAIdeiaIngenuaGanha() {
        assertThat(poda(2, "projecao").pontosVisitados()).isCloseTo(139.6, within(0.1));
        assertThat(poda(8, "projecao").pontosVisitados()).isCloseTo(8630.6, within(0.1));
        assertThat(poda(16, "projecao").pontosVisitados()).isCloseTo(17732.6, within(0.1));

        assertThat(poda(2, "projecao").pontosVisitados())
                .isGreaterThan(poda(2, "kd com poda").pontosVisitados());

        assertThat(poda(16, "projecao").pontosVisitados())
                .as("em dimensao 16 a ordem se inverte")
                .isLessThan(poda(16, "kd com poda").pontosVisitados());
    }

    private static Medidas.LinhaPoda poda(int dimensoes, String versao) {
        return PODA.stream()
                .filter(l -> l.dimensoes() == dimensoes && l.versao().equals(versao))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 4. a regra de corte ─────────────────────

    /**
     * A REGRA DE CORTE INVERTE DE SINAL, e inverte pela origem do alvo.
     *
     * <p>Na nuvem esticada em dimensao 2 com alvo do cubo, a regra esperta custa 7,765 vezes
     * a simples. Na mesma nuvem com alvo de dentro, custa 0,550. E em dimensao 8 com alvo de
     * dentro, custa 0,165.
     */
    @Test
    @DisplayName("na nuvem esticada a regra esperta perde por 7,8 e ganha por 6, conforme o alvo")
    void aRegraInverteDeSinal() {
        assertThat(regra(2, "esticada", "do cubo", "maior extensao").quantasVezes())
                .isCloseTo(7.765, within(0.01));

        assertThat(regra(2, "esticada", "de dentro", "maior extensao").quantasVezes())
                .isCloseTo(0.550, within(0.01));

        assertThat(regra(8, "esticada", "de dentro", "maior extensao").quantasVezes())
                .isCloseTo(0.165, within(0.01));

        assertThat(regra(2, "esticada", "do cubo", "ciclica").pontosVisitados())
                .isCloseTo(408.9, within(0.1));

        assertThat(regra(2, "esticada", "do cubo", "maior extensao").pontosVisitados())
                .isCloseTo(3175.0, within(0.1));
    }

    @Test
    @DisplayName("na nuvem uniforme as duas regras empatam, em qualquer dimensao e qualquer alvo")
    void naNuvemUniformeAsRegrasEmpatam() {
        for (int dimensoes : new int[] {2, 8})
            for (String origem : List.of("do cubo", "de dentro"))
                assertThat(regra(dimensoes, "uniforme", origem, "maior extensao").quantasVezes())
                        .as("dimensao " + dimensoes + ", alvo " + origem)
                        .isCloseTo(1.0, within(0.05));
    }

    private static Medidas.LinhaRegra regra(int dimensoes, String nuvem, String origem,
                                            String regra) {
        return REGRA.stream()
                .filter(l -> l.dimensoes() == dimensoes && l.nuvem().equals(nuvem)
                        && l.origemDoAlvo().equals(origem) && l.regra().equals(regra))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 5. de onde vem o alvo ─────────────────────

    /**
     * O QUE EU TINHA ESCRITO AO CONTRARIO.
     *
     * <p>Eu escrevi que uma nuvem agrupada e o melhor caso da poda. Com alvo sorteado no
     * cubo ela e o pior: 11.324 pontos visitados contra 1.190 da uniforme, dez vezes mais.
     */
    @Test
    @DisplayName("a nuvem agrupada e o pior caso da poda, e nao o melhor")
    void aNuvemAgrupadaEOPiorCaso() {
        assertThat(alvo("em grupos", 1_000_000).pontosVisitados()).isCloseTo(11324.4, within(0.1));
        assertThat(alvo("uniforme", 1_000_000).pontosVisitados()).isCloseTo(1190.7, within(0.1));

        assertThat(alvo("em grupos", 1_000_000).pontosVisitados()
                / alvo("uniforme", 1_000_000).pontosVisitados())
                .as("dez vezes pior, com alvo sorteado no cubo")
                .isGreaterThan(9.0);

        assertThat(alvo("em grupos", 500).pontosVisitados())
                .as("e com alvo de dentro ela volta a ser otima, com 33,5 pontos")
                .isCloseTo(33.5, within(0.1));
    }

    @Test
    @DisplayName("o custo cresce junto com o empurrao, em todas as tres nuvens")
    void oCustoSegueOEmpurrao() {
        for (String nuvem : List.of("uniforme", "em grupos", "esticada")) {
            List<Medidas.LinhaAlvo> daNuvem = ALVO.stream()
                    .filter(l -> l.nuvem().equals(nuvem) && l.origemDoAlvo().equals("de dentro"))
                    .toList();

            assertThat(daNuvem).hasSize(4);

            for (int i = 1; i < daNuvem.size(); i++)
                assertThat(daNuvem.get(i).pontosVisitados())
                        .as(nuvem + ", empurrao " + daNuvem.get(i).empurrao())
                        .isGreaterThan(daNuvem.get(i - 1).pontosVisitados());
        }

        assertThat(alvo("esticada", 500).pontosVisitados()).isCloseTo(30.8, within(0.1));
        assertThat(alvo("esticada", 100_000).pontosVisitados()).isCloseTo(5423.4, within(0.1));
    }

    /**
     * A unica linha fora de ordem da tabela, e ela tem explicacao.
     *
     * <p>Na nuvem uniforme, empurrar meio cubo custa MAIS do que sortear no cubo: 2.579,9
     * contra 1.190,7. Empurrado meio cubo a partir de um ponto, o alvo sai da caixa em boa
     * parte das vezes, e alvo fora da caixa e o caso mais caro que existe.
     */
    @Test
    @DisplayName("empurrar meio cubo custa mais que sortear no cubo, porque joga o alvo pra fora")
    void oEmpurraoGrandeJogaOAlvoParaFora() {
        assertThat(alvo("uniforme", 500_000).pontosVisitados()).isCloseTo(2579.9, within(0.1));

        assertThat(alvo("uniforme", 500_000).pontosVisitados())
                .isGreaterThan(alvo("uniforme", 1_000_000).pontosVisitados());
    }

    private static Medidas.LinhaAlvo alvo(String nuvem, int empurrao) {
        return ALVO.stream()
                .filter(l -> l.nuvem().equals(nuvem) && l.empurrao() == empurrao)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 6. k vizinhos ─────────────────────

    @Test
    @DisplayName("pedir dez de uma vez e muito mais barato do que pedir um dez vezes")
    void pedirMuitosDeUmaVezCompensa() {
        assertThat(kvizinhos(2, 1).pontosVisitados()).isCloseTo(17.4, within(0.1));
        assertThat(kvizinhos(2, 500).pontosVisitados()).isCloseTo(1091.0, within(0.1));

        assertThat(kvizinhos(2, 500).porVizinho())
                .as("quinhentos vizinhos custam 2,2 pontos cada, contra 17,4 de um sozinho")
                .isCloseTo(2.2, within(0.05));

        for (int dimensoes : new int[] {2, 8}) {
            List<Medidas.LinhaKvizinhos> daDimensao = KVIZINHOS.stream()
                    .filter(l -> l.dimensoes() == dimensoes)
                    .toList();

            for (int i = 1; i < daDimensao.size(); i++) {
                assertThat(daDimensao.get(i).pontosVisitados())
                        .as("mais vizinhos custa mais no total")
                        .isGreaterThan(daDimensao.get(i - 1).pontosVisitados());

                assertThat(daDimensao.get(i).porVizinho())
                        .as("e menos por vizinho")
                        .isLessThan(daDimensao.get(i - 1).porVizinho());
            }
        }
    }

    @Test
    @DisplayName("em dimensao 8, pedir 500 vizinhos ja e quase a forca bruta")
    void emDimensaoOitoPedirMuitosViraForcaBruta() {
        assertThat(kvizinhos(8, 500).fracaoDeN()).isGreaterThan(0.9);
        assertThat(kvizinhos(8, 500).pontosVisitados()).isCloseTo(18396.2, within(0.1));
    }

    private static Medidas.LinhaKvizinhos kvizinhos(int dimensoes, int k) {
        return KVIZINHOS.stream()
                .filter(l -> l.dimensoes() == dimensoes && l.k() == k)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 7. o tamanho da folha ─────────────────────

    /**
     * UMA MEDIDA SO NAO RESOLVE.
     *
     * <p>Folha de um ponto visita menos pontos que qualquer outra, em qualquer dimensao, e
     * mesmo assim e a pior escolha: ela abre cinco vezes mais nos. A soma das duas colunas
     * tem fundo no meio do caminho.
     */
    @Test
    @DisplayName("folha 1 visita menos pontos e abre mais nos, e perde na soma")
    void aFolhaDeUmGanhaNaColunaErrada() {
        assertThat(folha(2, 1).pontosVisitados()).isCloseTo(5.1, within(0.1));
        assertThat(folha(2, 256).pontosVisitados()).isCloseTo(182.0, within(0.1));

        for (int dimensoes : new int[] {2, 8}) {
            List<Medidas.LinhaFolha> daDimensao = FOLHA.stream()
                    .filter(l -> l.dimensoes() == dimensoes)
                    .toList();

            for (int i = 1; i < daDimensao.size(); i++) {
                assertThat(daDimensao.get(i).pontosVisitados())
                        .as("folha maior visita mais pontos")
                        .isGreaterThan(daDimensao.get(i - 1).pontosVisitados());

                assertThat(daDimensao.get(i).nosVisitados())
                        .as("e abre menos nos")
                        .isLessThan(daDimensao.get(i - 1).nosVisitados());
            }
        }
    }

    @Test
    @DisplayName("a soma tem fundo na folha 4 em dimensao 2 e na folha 16 em dimensao 8")
    void aSomaTemFundoNoMeio() {
        assertThat(melhorFolha(2)).isEqualTo(4);
        assertThat(melhorFolha(8)).isEqualTo(16);

        assertThat(folha(2, 4).somaDosDois()).isCloseTo(26.9, within(0.1));
        assertThat(folha(8, 16).somaDosDois()).isCloseTo(1509.7, within(0.1));

        assertThat(folha(2, 1).somaDosDois())
                .as("e a folha de um ponto perde do fundo nas duas dimensoes")
                .isGreaterThan(folha(2, 4).somaDosDois());

        assertThat(folha(8, 1).somaDosDois()).isGreaterThan(folha(8, 16).somaDosDois());
    }

    private static int melhorFolha(int dimensoes) {
        return FOLHA.stream()
                .filter(l -> l.dimensoes() == dimensoes)
                .min((a, b) -> Double.compare(a.somaDosDois(), b.somaDosDois()))
                .orElseThrow()
                .folha();
    }

    private static Medidas.LinhaFolha folha(int dimensoes, int folha) {
        return FOLHA.stream()
                .filter(l -> l.dimensoes() == dimensoes && l.folha() == folha)
                .findFirst()
                .orElseThrow();
    }
}
