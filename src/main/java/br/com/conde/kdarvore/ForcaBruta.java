package br.com.conde.kdarvore;

import java.util.ArrayList;
import java.util.List;

/**
 * A FORCA BRUTA: olha todos os pontos, sempre.
 *
 * <p>Ela e o juiz deste repositorio e tambem uma das versoes medidas, e as duas coisas pelo
 * mesmo motivo: nao ha o que errar nela. Ela nao poda, nao escolhe lado, nao tem invariante
 * para quebrar. O preco dela e conhecido e constante, n pontos por consulta, e esse numero
 * e a regua contra a qual toda coluna deste repositorio e lida.
 *
 * <p>E ela nao e uma versao de palha. Em dimensao alta ela VENCE, e a tabela da dimensao
 * mostra onde.
 */
public final class ForcaBruta implements Vizinhanca {

    private final int[][] pontos;
    private final Contador contador;

    public ForcaBruta(int[][] pontos, Contador contador) {
        this.pontos = pontos;
        this.contador = contador;
    }

    @Override
    public String nome() {
        return "forca bruta";
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

        for (int i = 0; i < pontos.length; i++) {
            contador.visitaPonto();
            melhores.oferecer(Distancia.aoQuadrado(pontos[i], alvo), i);
        }

        return melhores.emOrdem();
    }

    @Override
    public int[] numRaio(int[] alvo, long raioAoQuadrado) {
        contador.consulta();

        List<Integer> dentro = new ArrayList<>();

        for (int i = 0; i < pontos.length; i++) {
            contador.visitaPonto();

            if (Distancia.aoQuadrado(pontos[i], alvo) <= raioAoQuadrado) dentro.add(i);
        }

        int[] saida = new int[dentro.size()];

        for (int i = 0; i < saida.length; i++) saida[i] = dentro.get(i);

        return saida;
    }

    /** As distancias ate todos os pontos, em ordem crescente. So os testes usam. */
    public long[] todasAsDistancias(int[] alvo) {
        long[] distancias = new long[pontos.length];

        for (int i = 0; i < pontos.length; i++)
            distancias[i] = Distancia.aoQuadrado(pontos[i], alvo);

        java.util.Arrays.sort(distancias);

        return distancias;
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
