package br.com.conde.kdarvore;

/**
 * O CONTADOR: pontos visitados, nos visitados e podas. Nunca segundos.
 *
 * <p>O PONTO VISITADO e a medida certa e nao so a conveniente. Toda busca por vizinho mais
 * proximo, de qualquer jeito que seja escrita, termina comparando distancias ate pontos
 * concretos. A forca bruta visita todos; o que uma arvore k-d promete e visitar poucos. A
 * pergunta deste repositorio e quando ela cumpre.
 *
 * <p>E a medida tem um TETO conhecido, que e n. Isso e raro e e util: uma coluna de pontos
 * visitados nao precisa ser comparada com nada para ser lida, porque ela ja vem como
 * fracao do unico numero que importa. Visitar 19.400 de 20.000 pontos e uma frase completa.
 *
 * <p>Segundos nao serviriam: a arvore k-d gasta tempo em ponteiros e a forca bruta gasta
 * tempo num laco sobre um vetor contiguo, que o processador adora. Uma medida em segundos
 * misturaria a pergunta "quantos pontos preciso olhar" com a pergunta "quao rapido e olhar
 * um ponto", e so a primeira e sobre o algoritmo.
 */
public final class Contador {

    private long pontosVisitados;
    private long nosVisitados;
    private long podas;
    private long consultas;
    private long comparacoesDeCaixa;

    /** Um ponto teve a distancia ate o alvo calculada. */
    public void visitaPonto() {
        pontosVisitados++;
    }

    public void visitaPonto(long quantos) {
        pontosVisitados += quantos;
    }

    /** Um no da arvore foi aberto. */
    public void visitaNo() {
        nosVisitados++;
    }

    /** Uma subarvore inteira foi descartada sem ser aberta. */
    public void poda() {
        podas++;
    }

    /** A distancia ate o plano de corte foi comparada com o melhor raio de hoje. */
    public void comparaCaixa() {
        comparacoesDeCaixa++;
    }

    public void consulta() {
        consultas++;
    }

    public long pontosVisitados() {
        return pontosVisitados;
    }

    public long nosVisitados() {
        return nosVisitados;
    }

    public long podas() {
        return podas;
    }

    public long consultas() {
        return consultas;
    }

    public long comparacoesDeCaixa() {
        return comparacoesDeCaixa;
    }

    public double pontosPorConsulta() {
        return consultas == 0 ? 0 : (double) pontosVisitados / consultas;
    }

    public double nosPorConsulta() {
        return consultas == 0 ? 0 : (double) nosVisitados / consultas;
    }

    public double podasPorConsulta() {
        return consultas == 0 ? 0 : (double) podas / consultas;
    }

    public void zerar() {
        pontosVisitados = 0;
        nosVisitados = 0;
        podas = 0;
        consultas = 0;
        comparacoesDeCaixa = 0;
    }
}
