package br.com.conde.kdarvore;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * O JUIZ: a forca bruta.
 *
 * <p>Ela e o juiz certo e nao so o conveniente. Nao ha nela nenhuma invariante que possa
 * quebrar, nenhum lado a escolher, nenhuma subarvore a descartar: ela calcula as n
 * distancias e ordena. Um juiz que fosse uma segunda estrutura esperta poderia errar junto.
 *
 * <h2>O que ele compara, e o que ele NAO compara</h2>
 *
 * <p>Ele compara as DISTANCIAS, e nao so os indices. Com coordenadas inteiras, dois pontos
 * podem estar exatamente a mesma distancia do alvo, e nesse caso duas implementacoes
 * corretas devolvem indices diferentes sem que nenhuma esteja errada. Comparar indices
 * acusaria divergencia onde nao ha nenhuma, e o repositorio inteiro passaria a perseguir
 * um erro que nao existe.
 *
 * <p>Os indices entram na conferencia so depois do desempate estavel por indice, que todas
 * as implementacoes aplicam.
 */
public final class Juiz {

    private Juiz() {
    }

    public record Veredito(int consultas, int divergencias, String primeiraFalha) {

        public boolean passou() {
            return divergencias == 0;
        }
    }

    /**
     * Confere uma vizinhanca contra a forca bruta nas tres perguntas.
     *
     * <p>Os alvos sao sorteados no cubo inteiro, e isso inclui alvos LONGE de qualquer
     * ponto. Esse caso e o que mais quebra poda: com o alvo fora da nuvem, o primeiro
     * vizinho encontrado e ruim, o raio demora a apertar e um limite calculado errado deixa
     * passar.
     */
    public static Veredito julgar(Vizinhanca vizinhanca, int[][] pontos, int[][] alvos, int k) {
        ForcaBruta certo = new ForcaBruta(pontos, new Contador());

        int divergencias = 0;
        int consultas = 0;
        String primeiraFalha = null;

        for (int[] alvo : alvos) {
            consultas += 3;

            long[] distanciasCertas = distanciasDe(pontos, certo.maisProximos(alvo, k), alvo);
            long[] distanciasDele = distanciasDe(pontos, vizinhanca.maisProximos(alvo, k), alvo);

            if (!Arrays.equals(distanciasCertas, distanciasDele)) {
                divergencias++;

                if (primeiraFalha == null)
                    primeiraFalha = k + " vizinhos de " + Arrays.toString(alvo) + ": "
                            + Arrays.toString(distanciasDele) + " contra "
                            + Arrays.toString(distanciasCertas);
            }

            int meu = vizinhanca.maisProximo(alvo);
            int dele = certo.maisProximo(alvo);

            if (meu != dele) {
                divergencias++;

                if (primeiraFalha == null)
                    primeiraFalha = "mais proximo de " + Arrays.toString(alvo) + ": " + meu
                            + " contra " + dele;
            }

            /*
             * O RAIO E O DO TERCEIRO VIZINHO. Um raio fixo daria consultas vazias com uma
             * nuvem espalhada e a nuvem inteira com uma nuvem apertada, e nos dois casos
             * nao conferiria nada. Tirado da propria nuvem, ele sempre devolve um punhado.
             */
            long raio = distanciasCertas[Math.min(2, distanciasCertas.length - 1)];

            int[] meusNoRaio = vizinhanca.numRaio(alvo, raio);
            int[] delesNoRaio = certo.numRaio(alvo, raio);

            if (!Arrays.equals(meusNoRaio, delesNoRaio)) {
                divergencias++;

                if (primeiraFalha == null)
                    primeiraFalha = "no raio " + raio + ": " + meusNoRaio.length + " pontos contra "
                            + delesNoRaio.length;
            }
        }

        return new Veredito(consultas, divergencias, primeiraFalha);
    }

    private static long[] distanciasDe(int[][] pontos, int[] indices, int[] alvo) {
        long[] distancias = new long[indices.length];

        for (int i = 0; i < indices.length; i++)
            distancias[i] = Distancia.aoQuadrado(pontos[indices[i]], alvo);

        return distancias;
    }

    /** Pergunta o mais proximo de cada alvo, para medir o custo. */
    public static void perguntarTodos(Vizinhanca vizinhanca, int[][] alvos) {
        for (int[] alvo : alvos) vizinhanca.maisProximo(alvo);
    }

    /** As versoes que entram nas tabelas, cada uma com o nome que a tabela imprime. */
    public static Map<String, BiFunction<int[][], Contador, Vizinhanca>> asVersoes() {
        Map<String, BiFunction<int[][], Contador, Vizinhanca>> versoes = new LinkedHashMap<>();

        versoes.put("kd com poda", KdArvore::padrao);
        versoes.put("kd sem poda", (pontos, contador) ->
                new KdArvore(pontos, KdArvore.Regra.MAIOR_EXTENSAO, false, 16, contador));
        versoes.put("projecao", Projecao::new);
        versoes.put("forca bruta", ForcaBruta::new);

        return versoes;
    }
}
