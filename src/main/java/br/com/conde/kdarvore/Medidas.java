package br.com.conde.kdarvore;

import java.util.ArrayList;
import java.util.List;

/**
 * As MEDIDAS, em pontos visitados por consulta. Nunca segundos.
 *
 * <p>O PONTO VISITADO tem um teto conhecido, que e n, e e isso que faz dele a medida certa:
 * cada numero destas tabelas ja vem como fracao do unico valor que importa. Uma arvore k-d
 * que visita 19.400 de 20.000 pontos nao precisa ser comparada com nada para ser julgada.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros.
 */
public final class Medidas {

    private Medidas() {
    }

    /** Quantos pontos as tabelas grandes usam. */
    public static final int PONTOS = 20_000;

    /** Quantos alvos cada celula pergunta. */
    public static final int ALVOS = 200;

    public static final long SEMENTE = 20_251_011L;

    /** As dimensoes que a tabela da dimensao percorre. */
    public static final int[] DIMENSOES = {2, 3, 4, 6, 8, 10, 12, 16};

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String versao, String nuvem, int dimensoes, int pontos,
                                int consultas, int divergencias) {
    }

    /**
     * TODA VERSAO BATE COM A FORCA BRUTA, nas tres perguntas e nas tres nuvens.
     *
     * <p>Os alvos sao sorteados no cubo inteiro, e isso inclui alvos LONGE de qualquer
     * ponto. Esse caso e o que mais quebra poda: com o alvo fora da nuvem, o primeiro
     * vizinho encontrado e ruim, o raio demora a apertar e um limite mal calculado deixa
     * passar.
     */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> linhas = new ArrayList<>();
        int n = 2_000;

        for (int dimensoes : new int[] {2, 4, 8}) {
            int[][] alvos = Nuvens.alvos(40, dimensoes, SEMENTE + 99);

            for (var nuvem : Nuvens.todas(n, dimensoes, SEMENTE).entrySet())
                for (var versao : Juiz.asVersoes().entrySet()) {
                    Vizinhanca vizinhanca =
                            versao.getValue().apply(nuvem.getValue(), new Contador());

                    Juiz.Veredito veredito =
                            Juiz.julgar(vizinhanca, nuvem.getValue(), alvos, 5);

                    linhas.add(new LinhaCorrecao(versao.getKey(), nuvem.getKey(), dimensoes, n,
                            veredito.consultas(), veredito.divergencias()));
                }
        }

        return linhas;
    }

    // ───────────────────── 2. a dimensao ─────────────────────

    public record LinhaDimensao(int dimensoes, double doCubo, double fracaoDoCubo,
                                double deDentro, double fracaoDeDentro, double podas,
                                int altura, int nos) {
    }

    /**
     * A PERGUNTA DO REPOSITORIO: a partir de que dimensao a arvore k-d para de ajudar.
     *
     * <p>Em dimensao 2 ela visita uma fracao minuscula dos pontos. Em dimensao 16 ela visita
     * quase todos, e ainda paga os ponteiros, as caixas e as comparacoes de plano que a
     * forca bruta nao paga.
     *
     * <p>O motivo nao e da implementacao e nem da arvore: e do espaco. Em dimensao alta, o
     * vizinho mais proximo de um ponto nao e muito mais proximo do que o decimo, nem do que
     * o milesimo. O raio de poda chega apertado em duas dimensoes porque o vizinho esta
     * MUITO mais perto que o resto; em dezesseis, nao esta, e um raio frouxo nao descarta
     * caixa nenhuma.
     *
     * <p>A regra de bolso que sai da tabela e simples: com alvo sorteado no cubo, a arvore
     * so paga enquanto n for muito maior que 2 elevado a d.
     *
     * <h2>E a outra coluna</h2>
     *
     * <p>A tabela tem duas colunas de pontos visitados porque UMA ERA MENTIRA POR OMISSAO.
     * A primeira pergunta de um alvo sorteado no cubo inteiro; a segunda pergunta de um alvo
     * tirado de dentro da propria nuvem, com um empurrao pequeno.
     *
     * <p>Com alvo de dentro, a maldicao da dimensionalidade simplesmente nao aparece nestes
     * tamanhos: em dimensao 16 a arvore continua visitando algumas dezenas de pontos. O
     * vizinho mais proximo esta a uma distancia minuscula, o raio aperta na primeira folha e
     * tudo o mais morre podado.
     *
     * <p>Isso nao desmente a maldicao, delimita ela. O que fica caro em dimensao alta e
     * perguntar por um ponto que nao se parece com nada do conjunto, e consulta de verdade
     * quase nunca e assim: quem pergunta "quem se parece com isto" ja tem um isto que veio
     * do mesmo lugar que os dados.
     */
    public static List<LinhaDimensao> aDimensao() {
        List<LinhaDimensao> linhas = new ArrayList<>();

        for (int dimensoes : DIMENSOES) {
            int[][] pontos = Nuvens.uniforme(PONTOS, dimensoes, SEMENTE);

            Contador doCubo = new Contador();
            KdArvore arvore = KdArvore.padrao(pontos, doCubo);

            Juiz.perguntarTodos(arvore, Nuvens.alvos(ALVOS, dimensoes, SEMENTE + 99));

            Contador deDentro = new Contador();
            KdArvore outra = KdArvore.padrao(pontos, deDentro);

            Juiz.perguntarTodos(outra, Nuvens.alvosDeDentro(pontos, ALVOS, SEMENTE + 77));

            linhas.add(new LinhaDimensao(dimensoes,
                    doCubo.pontosPorConsulta(), doCubo.pontosPorConsulta() / PONTOS,
                    deDentro.pontosPorConsulta(), deDentro.pontosPorConsulta() / PONTOS,
                    doCubo.podasPorConsulta(), arvore.altura(), arvore.nos()));
        }

        return linhas;
    }

    // ───────────────────── 3. a poda ─────────────────────

    public record LinhaPoda(int dimensoes, String versao, double pontosVisitados,
                            double fracaoDeN) {
    }

    /**
     * A PODA E O ALGORITMO INTEIRO.
     *
     * <p>Sem poda, a arvore k-d visita os n pontos, igual a forca bruta, e ainda gasta a
     * descida. A estrutura nao economiza nada sozinha: ela so organiza o espaco de um jeito
     * que PERMITE descartar, e quem descarta e a comparacao entre a distancia ao plano de
     * corte e o raio do melhor vizinho de hoje.
     *
     * <p>A projecao num eixo e a mesma ideia com um limite mais fraco: ela usa uma dimensao
     * e ignora as outras. A tabela mostra o preco de ignorar.
     */
    public static List<LinhaPoda> aPoda() {
        List<LinhaPoda> linhas = new ArrayList<>();

        for (int dimensoes : new int[] {2, 4, 8, 16}) {
            int[][] pontos = Nuvens.uniforme(PONTOS, dimensoes, SEMENTE);
            int[][] alvos = Nuvens.alvos(ALVOS, dimensoes, SEMENTE + 99);

            for (var versao : Juiz.asVersoes().entrySet()) {
                Contador contador = new Contador();
                Vizinhanca vizinhanca = versao.getValue().apply(pontos, contador);

                Juiz.perguntarTodos(vizinhanca, alvos);

                linhas.add(new LinhaPoda(dimensoes, versao.getKey(),
                        contador.pontosPorConsulta(),
                        contador.pontosPorConsulta() / PONTOS));
            }
        }

        return linhas;
    }

    // ───────────────────── 4. a regra de corte ─────────────────────

    public record LinhaRegra(int dimensoes, String nuvem, String origemDoAlvo, String regra,
                             double pontosVisitados, double quantasVezes) {
    }

    /**
     * AS DUAS REGRAS DE CORTE, na nuvem uniforme e na nuvem esticada.
     *
     * <p>A regra ciclica corta por nivel, sem olhar os pontos: dimensao 0, depois 1, depois
     * 2. E a regra do artigo original e a que todo mundo escreve primeiro.
     *
     * <p>Eu esperava que a nuvem esticada separasse as duas, e que a ciclica perdesse feio
     * por gastar metade dos cortes em eixos onde quase nao ha o que separar. A coluna da
     * direita e quantas vezes a maior extensao custa em cima da ciclica.
     *
     * <p>Na nuvem uniforme e na agrupada elas empatam, como era de esperar. Na esticada em
     * dimensao 2, com alvo sorteado no cubo, a regra esperta perde por quase OITO VEZES.
     *
     * <p>Por isso esta tabela tem a origem do alvo como coluna. Nessa combinacao, os alvos
     * do cubo caem quase todos FORA da nuvem no eixo estreito, e ai cortar o eixo estreito
     * nao separa nada util: os dois lados ficam igualmente longe. A regra ciclica, que corta
     * o eixo largo em metade dos niveis aconteca o que acontecer, continua partindo a
     * distancia ao meio; a regra da maior extensao para de cortar o eixo largo assim que as
     * caixas ficam quadradas e gasta o resto em cortes que nao ajudam.
     */
    public static List<LinhaRegra> aRegra() {
        List<LinhaRegra> linhas = new ArrayList<>();

        for (int dimensoes : new int[] {2, 8})
            for (var nuvem : Nuvens.todas(PONTOS, dimensoes, SEMENTE).entrySet()) {
                int[][] pontos = nuvem.getValue();

                var origens = new java.util.LinkedHashMap<String, int[][]>();

                origens.put("do cubo", Nuvens.alvos(ALVOS, dimensoes, SEMENTE + 99));
                origens.put("de dentro", Nuvens.alvosDeDentro(pontos, ALVOS, SEMENTE + 77));

                for (var origem : origens.entrySet()) {
                    double daCiclica = 0;

                    for (KdArvore.Regra regra : KdArvore.Regra.values()) {
                        Contador contador = new Contador();
                        KdArvore arvore = new KdArvore(pontos, regra, true, 16, contador);

                        Juiz.perguntarTodos(arvore, origem.getValue());

                        double visitados = contador.pontosPorConsulta();

                        if (regra == KdArvore.Regra.CICLICA) daCiclica = visitados;

                        linhas.add(new LinhaRegra(dimensoes, nuvem.getKey(), origem.getKey(),
                                regra == KdArvore.Regra.CICLICA ? "ciclica" : "maior extensao",
                                visitados, visitados / daCiclica));
                    }
                }
            }

        return linhas;
    }

    // ───────────────────── 4b. de onde vem o alvo ─────────────────────

    public record LinhaAlvo(String nuvem, String origemDoAlvo, int empurrao,
                            double pontosVisitados, double fracaoDeN) {
    }

    /**
     * DE ONDE VEM O ALVO, que e o que eu tinha deixado de fora.
     *
     * <p>Eu escrevi que uma nuvem agrupada e o melhor caso da poda, porque o vizinho esta no
     * mesmo grupo e pertinho. Com alvos sorteados no cubo inteiro ela e o PIOR caso: quase
     * todo alvo cai no vazio entre os grupos, o vizinho mais proximo esta longe e o raio
     * nunca aperta.
     *
     * <p>Nao e a nuvem que decide, e a relacao entre o alvo e a nuvem. E essa relacao nao e
     * um interruptor de duas posicoes: a tabela percorre o caminho entre os dois extremos
     * empurrando o alvo para longe do ponto de onde ele saiu, de meio milesimo do cubo ate
     * o cubo inteiro.
     *
     * <p>O EMPURRAO E A UNICA COISA QUE MUDA entre as linhas de uma mesma nuvem. A arvore e
     * a mesma, os pontos sao os mesmos e a dimensao e a mesma.
     */
    public static List<LinhaAlvo> oAlvo() {
        List<LinhaAlvo> linhas = new ArrayList<>();
        int dimensoes = 8;
        int cubo = Distancia.MAIOR_COORDENADA;

        int[][] empurroes = {
                {cubo / 2_000, 0}, {cubo / 100, 0}, {cubo / 10, 0}, {cubo / 2, 0}
        };

        for (var nuvem : Nuvens.todas(PONTOS, dimensoes, SEMENTE).entrySet()) {
            int[][] pontos = nuvem.getValue();

            for (int[] empurrao : empurroes) {
                Contador contador = new Contador();
                KdArvore arvore = KdArvore.padrao(pontos, contador);

                Juiz.perguntarTodos(arvore,
                        Nuvens.alvosDeDentro(pontos, ALVOS, empurrao[0], SEMENTE + 77));

                linhas.add(new LinhaAlvo(nuvem.getKey(), "de dentro", empurrao[0],
                        contador.pontosPorConsulta(), contador.pontosPorConsulta() / PONTOS));
            }

            Contador doCubo = new Contador();
            KdArvore arvore = KdArvore.padrao(pontos, doCubo);

            Juiz.perguntarTodos(arvore, Nuvens.alvos(ALVOS, dimensoes, SEMENTE + 99));

            linhas.add(new LinhaAlvo(nuvem.getKey(), "do cubo", cubo,
                    doCubo.pontosPorConsulta(), doCubo.pontosPorConsulta() / PONTOS));
        }

        return linhas;
    }

    // ───────────────────── 5. k vizinhos ─────────────────────

    public record LinhaKvizinhos(int dimensoes, int k, double pontosVisitados,
                                 double fracaoDeN, double porVizinho) {
    }

    /**
     * O CUSTO DE PEDIR K VIZINHOS EM VEZ DE UM.
     *
     * <p>Pedir mais vizinhos custa duas vezes: o raio de poda passa a ser o do k-esimo, que
     * e maior, e a consulta comeca CEGA, sem poder podar nada, ate juntar os primeiros k.
     *
     * <p>A coluna da direita e o custo por vizinho entregue, e ela cai: pedir dez de uma vez
     * e muito mais barato do que pedir um dez vezes.
     */
    public static List<LinhaKvizinhos> osKvizinhos() {
        List<LinhaKvizinhos> linhas = new ArrayList<>();

        for (int dimensoes : new int[] {2, 8}) {
            int[][] pontos = Nuvens.uniforme(PONTOS, dimensoes, SEMENTE);
            int[][] alvos = Nuvens.alvos(ALVOS, dimensoes, SEMENTE + 99);

            for (int k : new int[] {1, 5, 20, 100, 500}) {
                Contador contador = new Contador();
                KdArvore arvore = KdArvore.padrao(pontos, contador);

                for (int[] alvo : alvos) arvore.maisProximos(alvo, k);

                linhas.add(new LinhaKvizinhos(dimensoes, k, contador.pontosPorConsulta(),
                        contador.pontosPorConsulta() / PONTOS,
                        contador.pontosPorConsulta() / k));
            }
        }

        return linhas;
    }

    // ───────────────────── 6. o tamanho da folha ─────────────────────

    public record LinhaFolha(int dimensoes, int folha, double pontosVisitados,
                             double nosVisitados, double somaDosDois, int nos) {
    }

    /**
     * O TAMANHO DA FOLHA: quantos pontos ficam juntos no fim da arvore.
     *
     * <p>Folha de um ponto visita o menor numero possivel de pontos, porque corta ate o
     * fim. E e a pior escolha mesmo assim: cada corte a mais e um no a abrir, uma caixa a
     * comparar, um ponteiro a seguir, e a soma das duas colunas tem um fundo no meio do
     * caminho.
     *
     * <p>Esta e a tabela que mostra por que uma medida so nao resolve. Olhando so os pontos
     * visitados, folha 1 ganha sempre e a conclusao seria errada.
     */
    public static List<LinhaFolha> aFolha() {
        List<LinhaFolha> linhas = new ArrayList<>();

        for (int dimensoes : new int[] {2, 8}) {
            int[][] pontos = Nuvens.uniforme(PONTOS, dimensoes, SEMENTE);
            int[][] alvos = Nuvens.alvos(ALVOS, dimensoes, SEMENTE + 99);

            for (int folha : new int[] {1, 4, 16, 64, 256}) {
                Contador contador = new Contador();
                KdArvore arvore = new KdArvore(pontos, KdArvore.Regra.MAIOR_EXTENSAO, true,
                        folha, contador);

                Juiz.perguntarTodos(arvore, alvos);

                linhas.add(new LinhaFolha(dimensoes, folha, contador.pontosPorConsulta(),
                        contador.nosPorConsulta(),
                        contador.pontosPorConsulta() + contador.nosPorConsulta(),
                        arvore.nos()));
            }
        }

        return linhas;
    }
}
