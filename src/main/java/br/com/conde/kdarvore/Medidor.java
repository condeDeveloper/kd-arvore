package br.com.conde.kdarvore;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("dimensao") || comando.equals("tudo")) dimensao();
        if (comando.equals("poda") || comando.equals("tudo")) poda();
        if (comando.equals("regra") || comando.equals("tudo")) regra();
        if (comando.equals("alvo") || comando.equals("tudo")) alvo();
        if (comando.equals("kvizinhos") || comando.equals("tudo")) kvizinhos();
        if (comando.equals("folha") || comando.equals("tudo")) folha();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("TODA VERSAO BATE COM A FORCA BRUTA, nas tres perguntas.");
        System.out.println();

        var linhas = Medidas.aCorrecao();
        long erradas = linhas.stream().filter(l -> l.divergencias() > 0).count();

        System.out.printf("  %-16s %-12s %12s %10s %12s %14s%n",
                "versao", "nuvem", "dimensoes", "pontos", "consultas", "divergencias");

        for (var l : linhas)
            System.out.printf("  %-16s %-12s %12d %10d %12d %14d%n",
                    l.versao(), l.nuvem(), l.dimensoes(), l.pontos(), l.consultas(),
                    l.divergencias());

        System.out.println();
        System.out.printf("  %d combinacoes, %d com divergencia.%n", linhas.size(), erradas);
        System.out.println();
    }

    private static void dimensao() {
        System.out.println();
        System.out.println("A PARTIR DE QUE DIMENSAO A ARVORE PARA DE AJUDAR, com "
                + Medidas.PONTOS + " pontos.");
        System.out.println();
        System.out.printf("  %10s %18s %14s %18s %14s %18s%n",
                "dimensoes", "alvo do cubo", "fracao de n", "alvo de dentro", "fracao de n",
                "podas por consulta");

        for (var l : Medidas.aDimensao())
            System.out.printf(Locale.ROOT, "  %10d %18.1f %14.4f %18.1f %14.4f %18.1f%n",
                    l.dimensoes(), l.doCubo(), l.fracaoDoCubo(), l.deDentro(),
                    l.fracaoDeDentro(), l.podas());

        System.out.println();
    }

    private static void poda() {
        System.out.println();
        System.out.println("A PODA E O ALGORITMO INTEIRO, com " + Medidas.PONTOS + " pontos.");
        System.out.println();
        System.out.printf("  %10s %-16s %20s %16s%n",
                "dimensoes", "versao", "pontos visitados", "fracao de n");

        for (var l : Medidas.aPoda())
            System.out.printf(Locale.ROOT, "  %10d %-16s %20.1f %16.4f%n",
                    l.dimensoes(), l.versao(), l.pontosVisitados(), l.fracaoDeN());

        System.out.println();
    }

    private static void regra() {
        System.out.println();
        System.out.println("AS DUAS REGRAS DE CORTE, e o quanto elas mudam.");
        System.out.println();
        System.out.printf("  %10s %-12s %-12s %-18s %20s %22s%n",
                "dimensoes", "nuvem", "alvo", "regra", "pontos visitados",
                "vezes a ciclica");

        for (var l : Medidas.aRegra())
            System.out.printf(Locale.ROOT, "  %10d %-12s %-12s %-18s %20.1f %22.3f%n",
                    l.dimensoes(), l.nuvem(), l.origemDoAlvo(), l.regra(), l.pontosVisitados(),
                    l.quantasVezes());

        System.out.println();
    }

    private static void alvo() {
        System.out.println();
        System.out.println("DE ONDE VEM O ALVO, em dimensao 8.");
        System.out.println();
        System.out.printf("  %-12s %-12s %14s %20s %16s%n",
                "nuvem", "alvo", "empurrao", "pontos visitados", "fracao de n");

        for (var l : Medidas.oAlvo())
            System.out.printf(Locale.ROOT, "  %-12s %-12s %14d %20.1f %16.4f%n",
                    l.nuvem(), l.origemDoAlvo(), l.empurrao(), l.pontosVisitados(),
                    l.fracaoDeN());

        System.out.println();
    }

    private static void kvizinhos() {
        System.out.println();
        System.out.println("O CUSTO DE PEDIR K VIZINHOS EM VEZ DE UM.");
        System.out.println();
        System.out.printf("  %10s %8s %20s %16s %18s%n",
                "dimensoes", "k", "pontos visitados", "fracao de n", "por vizinho");

        for (var l : Medidas.osKvizinhos())
            System.out.printf(Locale.ROOT, "  %10d %8d %20.1f %16.4f %18.1f%n",
                    l.dimensoes(), l.k(), l.pontosVisitados(), l.fracaoDeN(), l.porVizinho());

        System.out.println();
    }

    private static void folha() {
        System.out.println();
        System.out.println("O TAMANHO DA FOLHA: pontos visitados contra nos abertos.");
        System.out.println();
        System.out.printf("  %10s %8s %20s %16s %16s %10s%n",
                "dimensoes", "folha", "pontos visitados", "nos abertos", "soma dos dois", "nos");

        for (var l : Medidas.aFolha())
            System.out.printf(Locale.ROOT, "  %10d %8d %20.1f %16.1f %16.1f %10d%n",
                    l.dimensoes(), l.folha(), l.pontosVisitados(), l.nosVisitados(),
                    l.somaDosDois(), l.nos());

        System.out.println();
    }
}
