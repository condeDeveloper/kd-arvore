package br.com.conde.kdarvore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A ARVORE K-D, com a poda e a regra de corte em interruptores.
 *
 * <p>Uma classe so para todas as versoes, porque a descida e a mesma e e na descida que o
 * erro mora. Separadas, elas repetiriam a busca inteira para trocar uma linha.
 *
 * <h2>O que ela faz</h2>
 *
 * <p>Corta o espaco por um plano perpendicular a um dos eixos, na MEDIANA dos pontos
 * daquele lado. Cortar na mediana e o que garante que a arvore tem altura logaritmica
 * sempre, com qualquer nuvem de pontos: nao existe ordem de insercao ruim aqui porque nao
 * existe insercao, a arvore e montada de uma vez sobre todos os pontos.
 *
 * <p>Quem procura desce primeiro pelo lado em que o alvo esta, porque so assim ela chega
 * rapido a um vizinho razoavelmente bom. O raio desse vizinho e o que permite descartar o
 * outro lado sem abri-lo.
 */
public final class KdArvore implements Vizinhanca {

    /** Como escolher o eixo do corte. */
    public enum Regra {

        /**
         * Rodar as dimensoes por nivel: 0, 1, 2, ... e volta ao 0.
         *
         * <p>E a regra do artigo original e a que todo mundo escreve primeiro. Ela nao olha
         * para os pontos, so para a profundidade.
         */
        CICLICA,

        /**
         * Cortar o eixo em que os pontos daquele lado estao mais espalhados.
         *
         * <p>Custa uma varredura por no na montagem e produz caixas mais cubicas, que e o
         * que faz a poda funcionar: uma caixa comprida tem muita area perto do alvo e e
         * descartada com dificuldade.
         */
        MAIOR_EXTENSAO
    }

    private static final class No {
        int dimensao;
        int corte;
        int de;
        int ate;
        boolean folha;
        No esquerda;
        No direita;
    }

    private final int[][] pontos;
    private final int[] ordem;
    private final int dimensoes;
    private final Regra regra;
    private final boolean podar;
    private final int tamanhoDaFolha;
    private final Contador contador;

    private No raiz;
    private int quantosNos;
    private int altura;

    public KdArvore(int[][] pontos, Regra regra, boolean podar, int tamanhoDaFolha,
                    Contador contador) {

        if (tamanhoDaFolha < 1) throw new IllegalArgumentException("folha de pelo menos um ponto");

        this.pontos = pontos;
        this.dimensoes = pontos.length == 0 ? 1 : pontos[0].length;
        this.regra = regra;
        this.podar = podar;
        this.tamanhoDaFolha = tamanhoDaFolha;
        this.contador = contador;
        this.ordem = new int[pontos.length];

        for (int i = 0; i < pontos.length; i++) ordem[i] = i;

        montar();
    }

    public static KdArvore padrao(int[][] pontos, Contador contador) {
        return new KdArvore(pontos, Regra.MAIOR_EXTENSAO, true, 16, contador);
    }

    @Override
    public String nome() {
        return "kd " + (regra == Regra.CICLICA ? "ciclica" : "maior extensao")
                + (podar ? "" : " sem poda") + " folha " + tamanhoDaFolha;
    }

    public int tamanhoDaFolha() {
        return tamanhoDaFolha;
    }

    // ───────────────────── montar ─────────────────────

    private static final class Quadro {
        final No pai;
        final boolean esquerda;
        final int de;
        final int ate;
        final int profundidade;

        Quadro(No pai, boolean esquerda, int de, int ate, int profundidade) {
            this.pai = pai;
            this.esquerda = esquerda;
            this.de = de;
            this.ate = ate;
            this.profundidade = profundidade;
        }
    }

    /**
     * A montagem com PILHA EXPLICITA.
     *
     * <p>O corte na mediana garante altura logaritmica, entao a pilha de chamadas caberia.
     * A pilha explicita esta aqui porque a montagem tem que ligar cada no ao pai, e com ela
     * essa ligacao e um campo do quadro em vez de um valor de retorno: fica visivel que a
     * arvore e construida de cima para baixo sobre uma unica permutacao dos indices, e que
     * nenhum ponto e copiado.
     */
    private void montar() {
        if (pontos.length == 0) return;

        Deque<Quadro> pilha = new ArrayDeque<>();

        pilha.push(new Quadro(null, false, 0, pontos.length, 1));

        while (!pilha.isEmpty()) {
            Quadro quadro = pilha.pop();
            No no = criar(quadro.de, quadro.ate, quadro.profundidade);

            quantosNos++;
            altura = Math.max(altura, quadro.profundidade);

            if (quadro.pai == null) raiz = no;
            else if (quadro.esquerda) quadro.pai.esquerda = no;
            else quadro.pai.direita = no;

            if (no.folha) continue;

            int meio = quadro.de + (quadro.ate - quadro.de) / 2;

            pilha.push(new Quadro(no, true, quadro.de, meio, quadro.profundidade + 1));
            pilha.push(new Quadro(no, false, meio, quadro.ate, quadro.profundidade + 1));
        }
    }

    private No criar(int de, int ate, int profundidade) {
        No no = new No();

        no.de = de;
        no.ate = ate;

        if (ate - de <= tamanhoDaFolha) {
            no.folha = true;
            return no;
        }

        no.dimensao = regra == Regra.CICLICA
                ? (profundidade - 1) % dimensoes
                : dimensaoDeMaiorExtensao(de, ate);

        int meio = de + (ate - de) / 2;

        selecionar(de, ate, meio, no.dimensao);

        no.corte = pontos[ordem[meio]][no.dimensao];

        return no;
    }

    private int dimensaoDeMaiorExtensao(int de, int ate) {
        int melhor = 0;
        long maiorExtensao = -1;

        for (int d = 0; d < dimensoes; d++) {
            int menor = Integer.MAX_VALUE;
            int maior = Integer.MIN_VALUE;

            for (int i = de; i < ate; i++) {
                int valor = pontos[ordem[i]][d];

                if (valor < menor) menor = valor;
                if (valor > maior) maior = valor;
            }

            long extensao = (long) maior - menor;

            if (extensao > maiorExtensao) {
                maiorExtensao = extensao;
                melhor = d;
            }
        }

        return melhor;
    }

    /**
     * Poe o k-esimo menor no lugar k, sem ordenar o resto.
     *
     * <p>Ordenar o intervalo inteiro daria a mesma arvore e custaria um logaritmo a mais por
     * no. O que a arvore precisa e so que tudo a esquerda de k seja menor ou igual e tudo a
     * direita seja maior ou igual, e essa e exatamente a promessa da selecao.
     *
     * <p>A particao e a de Hoare com pivo no meio, e o laco e explicito ate o fim: a
     * recursao de cauda vira um ajuste de intervalo.
     */
    private void selecionar(int de, int ate, int k, int dimensao) {
        int baixo = de;
        int alto = ate - 1;

        while (baixo < alto) {
            int pivo = pontos[ordem[baixo + (alto - baixo) / 2]][dimensao];
            int i = baixo;
            int j = alto;

            while (i <= j) {
                while (pontos[ordem[i]][dimensao] < pivo) i++;
                while (pontos[ordem[j]][dimensao] > pivo) j--;

                if (i <= j) {
                    int guarda = ordem[i];

                    ordem[i] = ordem[j];
                    ordem[j] = guarda;
                    i++;
                    j--;
                }
            }

            if (k <= j) alto = j;
            else if (k >= i) baixo = i;
            else return;
        }
    }

    // ───────────────────── procurar ─────────────────────

    @Override
    public int maisProximo(int[] alvo) {
        int[] um = maisProximos(alvo, 1);

        return um.length == 0 ? -1 : um[0];
    }

    private static final class Pendente {
        final No no;
        final long limite;

        Pendente(No no, long limite) {
            this.no = no;
            this.limite = limite;
        }
    }

    @Override
    public int[] maisProximos(int[] alvo, int k) {
        contador.consulta();

        Melhores melhores = new Melhores(k);

        if (raiz == null) return melhores.emOrdem();

        Deque<Pendente> pilha = new ArrayDeque<>();

        pilha.push(new Pendente(raiz, 0));

        while (!pilha.isEmpty()) {
            Pendente pendente = pilha.pop();

            /*
             * A PODA ACONTECE AQUI, NA SAIDA DA PILHA E NAO NA ENTRADA.
             *
             * O limite foi calculado quando este no foi empilhado, mas o raio de poda
             * melhora enquanto o lado de perto e explorado. Conferir agora usa o raio mais
             * apertado que existe, e e dai que vem quase toda a poda: um no empilhado com
             * esperanca morre sem ser aberto.
             */
            if (podar && pendente.limite > melhores.pior()) {
                contador.poda();
                continue;
            }

            No no = pendente.no;

            contador.visitaNo();

            if (no.folha) {
                for (int i = no.de; i < no.ate; i++) {
                    int indice = ordem[i];

                    contador.visitaPonto();
                    melhores.oferecer(Distancia.aoQuadrado(pontos[indice], alvo), indice);
                }
                continue;
            }

            long aoPlano = Distancia.ateOPlano(alvo, no.dimensao, no.corte);
            boolean paraEsquerda = alvo[no.dimensao] < no.corte;

            No perto = paraEsquerda ? no.esquerda : no.direita;
            No longe = paraEsquerda ? no.direita : no.esquerda;

            contador.comparaCaixa();

            /*
             * O LADO DE LONGE ENTRA PRIMEIRO PARA SAIR POR ULTIMO. A pilha inverte a ordem,
             * e a ordem importa: o lado de perto precisa ser explorado antes para que o raio
             * ja esteja apertado quando o lado de longe for considerado.
             */
            pilha.push(new Pendente(longe, aoPlano));
            pilha.push(new Pendente(perto, pendente.limite));
        }

        return melhores.emOrdem();
    }

    @Override
    public int[] numRaio(int[] alvo, long raioAoQuadrado) {
        contador.consulta();

        List<Integer> dentro = new ArrayList<>();

        if (raiz == null) return new int[0];

        Deque<Pendente> pilha = new ArrayDeque<>();

        pilha.push(new Pendente(raiz, 0));

        while (!pilha.isEmpty()) {
            Pendente pendente = pilha.pop();

            /*
             * Aqui o raio NAO melhora durante a consulta: ele foi dado de fora e e fixo.
             * Entao a poda nao ganha nada em conferir na saida e podia conferir na entrada;
             * a conferencia fica no mesmo lugar para que as duas buscas tenham a mesma
             * forma e uma nao divirja da outra por descuido.
             */
            if (podar && pendente.limite > raioAoQuadrado) {
                contador.poda();
                continue;
            }

            No no = pendente.no;

            contador.visitaNo();

            if (no.folha) {
                for (int i = no.de; i < no.ate; i++) {
                    int indice = ordem[i];

                    contador.visitaPonto();

                    if (Distancia.aoQuadrado(pontos[indice], alvo) <= raioAoQuadrado)
                        dentro.add(indice);
                }
                continue;
            }

            long aoPlano = Distancia.ateOPlano(alvo, no.dimensao, no.corte);
            boolean paraEsquerda = alvo[no.dimensao] < no.corte;

            contador.comparaCaixa();

            pilha.push(new Pendente(paraEsquerda ? no.direita : no.esquerda, aoPlano));
            pilha.push(new Pendente(paraEsquerda ? no.esquerda : no.direita, pendente.limite));
        }

        int[] saida = new int[dentro.size()];

        for (int i = 0; i < saida.length; i++) saida[i] = dentro.get(i);

        java.util.Arrays.sort(saida);

        return saida;
    }

    @Override
    public int nos() {
        return quantosNos;
    }

    @Override
    public int altura() {
        return altura;
    }

    /**
     * Confere as invariantes da arvore. So os testes usam.
     *
     * <p>Toda folha com no maximo o tamanho combinado, todo ponto num no so, e, em cada
     * corte, todo ponto da esquerda com coordenada menor ou igual ao corte e todo ponto da
     * direita com coordenada maior ou igual.
     *
     * <p>Sem a ultima, a arvore ainda responderia certo SEM poda e erraria COM poda, que e
     * o jeito mais traicoeiro de uma arvore k-d quebrar: a resposta errada so aparece na
     * versao rapida.
     */
    public void conferirInvariantes() {
        boolean[] visto = new boolean[pontos.length];

        if (raiz != null) conferir(raiz, visto);

        for (int i = 0; i < pontos.length; i++)
            if (!visto[i]) throw new IllegalStateException("o ponto " + i + " sumiu da arvore");
    }

    private void conferir(No no, boolean[] visto) {
        if (no.folha) {
            if (no.ate - no.de > tamanhoDaFolha)
                throw new IllegalStateException("folha com " + (no.ate - no.de) + " pontos");

            for (int i = no.de; i < no.ate; i++) {
                if (visto[ordem[i]])
                    throw new IllegalStateException("o ponto " + ordem[i] + " esta em duas folhas");

                visto[ordem[i]] = true;
            }

            return;
        }

        int meio = no.de + (no.ate - no.de) / 2;

        for (int i = no.de; i < meio; i++)
            if (pontos[ordem[i]][no.dimensao] > no.corte)
                throw new IllegalStateException("ponto da esquerda acima do corte");

        for (int i = meio; i < no.ate; i++)
            if (pontos[ordem[i]][no.dimensao] < no.corte)
                throw new IllegalStateException("ponto da direita abaixo do corte");

        conferir(no.esquerda, visto);
        conferir(no.direita, visto);
    }
}
