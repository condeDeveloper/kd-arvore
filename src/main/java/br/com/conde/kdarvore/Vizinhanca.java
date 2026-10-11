package br.com.conde.kdarvore;

/**
 * Responder quem esta perto. Tres perguntas, e a terceira e a que mais custa.
 *
 * <p>Toda implementacao daqui responde as tres de um jeito EXATO. Nenhuma delas aproxima,
 * e isso e deliberado: busca aproximada de vizinho e outro assunto, e ele costuma entrar na
 * conversa exatamente no ponto em que a busca exata para de valer a pena. Aqui a ideia e
 * medir esse ponto, nao fugir dele.
 */
public interface Vizinhanca {

    /** O indice do ponto mais proximo do alvo. Empate resolve pelo menor indice. */
    int maisProximo(int[] alvo);

    /** Os k mais proximos, do mais perto ao mais longe. */
    int[] maisProximos(int[] alvo, int k);

    /** Todos os pontos a distancia ao quadrado menor ou igual ao raio, em ordem de indice. */
    int[] numRaio(int[] alvo, long raioAoQuadrado);

    int nos();

    int altura();

    String nome();
}
