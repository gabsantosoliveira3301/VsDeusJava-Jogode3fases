import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * VsDeusJava 0.1.0V
 *
 * Eu dividi o projeto em alguns arquivos para não deixar tudo misturado:
 * - JogoCartas cuida das regras, das perguntas e da janela;
 * - Mesa3D monta o cenário e posiciona os objetos;
 * - Texturas desenha as imagens que aparecem nas cartas e na sala;
 * - Animacao controla os movimentos da câmera, das velas e da porta;
 * - Fala cuida do texto que aparece aos poucos;
 * - Render3D transforma os objetos 3D em pixels na imagem.
 *
 * A cena não depende de imagens externas nem de uma engine pronta. As formas
 * e texturas são montadas pelo próprio Java durante a execução.
 */
public class JogoCartas extends JFrame {
    // Só uso tons de cinza na interface, então deixei esta continha num lugar só.
    static Color cinza(int tom) { return new Color(tom,tom,tom); }

    // ===== 1. REGRAS DO JOGO ================================================
    // A partida muda de fase. Isso ajuda a liberar só os botões que fazem sentido
    // naquele momento, em vez de deixar qualquer ação disponível o tempo todo.
    private enum Fase { INICIO, DUELO, REVELACAO, PORTA, JAVA, CURIOSIDADES, RESULTADO }
    // Mantive as cartas nesta ordem porque os números também entram na conta do duelo:
    // 0 = pedra, 1 = papel e 2 = tesoura. Se mudar aqui, tem que rever a conta lá embaixo.
    private static final String[] CARTAS = {"PEDRA", "PAPEL", "TESOURA"};

    // Para não criar uma classe só para guardar perguntas, usei matrizes mesmo.
    // Em cada linha, a posição 0 é a pergunta e as posições 1, 2 e 3 são respostas.
    // A primeira resposta do banco é a certa; depois eu embaralho a ordem dos botões.
    private static final String[][] JAVA = {
        {"Qual palavra cria uma classe em Java?", "class", "define", "struct"},
        {"Qual método inicia um programa Java?", "main", "start", "run"},
        {"Qual tipo guarda verdadeiro ou falso?", "boolean", "int", "char"},
        {"Como se declara uma constante?", "final", "static", "fixed"},
        {"Qual símbolo termina uma instrução?", ";", ":", "."}
    };
    // Esse segundo banco usa exatamente o mesmo formato do quiz de Java.
    private static final String[][] CURIOSIDADES = {
        {"Qual planeta é conhecido como planeta vermelho?", "Marte", "Vênus", "Netuno"},
        {"Quantos lados tem um hexágono?", "Seis", "Cinco", "Oito"},
        {"Qual é o maior oceano da Terra?", "Pacífico", "Atlântico", "Índico"},
        {"Qual destes animais é mamífero?", "Golfinho", "Tubarão", "Polvo"},
        {"Qual é a fórmula química da água?", "H2O", "CO2", "O2"},
        {"Em que continente fica o Egito?", "África", "Europa", "Ásia"},
        {"Quantos minutos há em uma hora?", "60", "100", "30"},
        {"Qual instrumento mede temperatura?", "Termômetro", "Barômetro", "Bússola"},
        {"Qual é um satélite natural da Terra?", "Lua", "Marte", "Sol"},
        {"Azul com amarelo resulta em qual cor?", "Verde", "Roxo", "Laranja"}
    };
    // Uso um gerador só para os sorteios do duelo, das falas e das respostas.
    private final Random acaso = new Random();
    // O ranking fica na memória enquanto o jogo está aberto; não salvo em arquivo.
    private final List<Resultado> ranking = new ArrayList<Resultado>();
    private Fase fase = Fase.INICIO;
    private String jogador = "Viajante", mensagem = "Sente-se, viajante. Diga seu nome... quero saber quem ousa desafiar o arquivo.";
    // Guardo aqui o andamento da partida. pontosJava fica separado para conferir
    // a nota da prova de Java, sem misturar com os pontos do duelo e das curiosidades.
    // rodada conta só partidas que não deram empate; escolha/casa começam em -1
    // porque ainda não existe carta selecionada nem carta revelada.
    private int pontos, pontosJava, rodada, vitorias, pergunta, escolha = -1, casa = -1;
    // Cada posição diz qual resposta original foi parar no botão A, B ou C.
    private int[] ordem = {0, 1, 2};
    // naSalaQuiz muda o cenário desenhado; respostaPendente impede responder duas vezes
    // à mesma pergunta antes de clicar em "próxima pergunta".
    private boolean naSalaQuiz, respostaPendente;

    // Cada vez que alguém termina uma partida, guardo nome e pontuação neste objeto.
    private static class Resultado {
        final String nome; final int pontos;
        Resultado(String nome, int pontos) { this.nome = nome; this.pontos = pontos; }
    }

    // Começar outra partida é basicamente limpar o que ficou da anterior e voltar ao duelo.
    private void iniciar() {
        String nome = nomeCampo.getText().trim();
        jogador = nome.isEmpty() ? "Viajante" : nome.substring(0, Math.min(20, nome.length()));
        pontos = pontosJava = rodada = vitorias = pergunta = 0;
        naSalaQuiz=respostaPendente=false;
        escolha = casa = -1;
        cena.animacao.reiniciar();
        fase = Fase.DUELO;
        mensagem = "Vamos começar devagar. Escolha sua carta e confirme. Duas vitórias... e eu abro a próxima porta.";
        atualizarTela();
    }

    // Aqui só marco a carta escolhida. A carta da casa ainda não é sorteada.
    private void selecionar(int carta) {
        if (fase != Fase.DUELO) return;
        escolha = carta;
        atualizarTela();
    }

    // Só resolvo a rodada quando o jogador confirma. Primeiro vejo se empatou;
    // se não, a conta circular das cartas diz quem venceu sem vários ifs separados.
    // Com os índices na ordem pedra/papel/tesoura, o resto 1 significa vitória do jogador.
    private void jogar() {
        if (fase != Fase.DUELO || escolha < 0) return;
        casa = sortearCasa(acaso, escolha);
        if (escolha == casa) mensagem = comentario(
            "A mesma escolha. As velas permanecem... jogue outra vez.",
            "Empate. Nem você, nem eu. As chamas continuam acesas... vamos repetir.");
        else if ((escolha - casa + 3) % 3 == 1) {
            vitorias++; pontos += 15;
            mensagem = vitorias == 2
                ? "Minhas duas chamas se foram. O duelo é seu. Mais 15 pontos... pode passar."
                : comentario("Uma das minhas chamas se foi... mais 15 pontos para você.",
                             "A balança está do seu lado. Mais 15 pontos... ainda tenho uma chama.");
        } else mensagem = rodada-vitorias >= 1
            ? "Sua última chama se apagou. Esta partida é minha... mas você pode sentar-se outra vez."
            : comentario("Esta foi minha. Uma de suas velas se apagou... ainda há tempo para virar o jogo.",
                         "Minha carta levou a melhor. Não desista... sua outra vela ainda está acesa.");
        mensagem = comentarioCarta(escolha) + mensagem;
        if (escolha != casa) rodada++;
        fase = Fase.REVELACAO;
        cena.animacao.revelar(vitorias, rodada-vitorias);
        atualizarTela();
    }

    // Dei uma ajudinha no sorteio: 60% de chance de a casa perder, 20% de empatar
    // e 20% de ganhar. Não é uma IA que analisa a jogada, são chances que defini.
    // A ordem pedra/papel/tesoura é importante para a conta com resto da divisão.
    static int sortearCasa(Random sorteio, int escolha) {
        int resultado = sorteio.nextInt(100);
        if (resultado < 60) return (escolha + 2) % 3;
        if (resultado < 80) return escolha;
        return (escolha + 1) % 3;
    }

    // É só um detalhe de personalidade do personagem, não muda a pontuação.
    static String comentarioCarta(int carta) {
        String[] reacoes = {"Pedra... firme e pesada. ", "Papel... tão frágil, e tão perigoso. ",
                            "Tesoura... um corte preciso. "};
        return reacoes[carta];
    }

    // Quando tenho mais de uma fala possível, escolho uma para não ficar repetitivo.
    private String comentario(String... falas) { return falas[acaso.nextInt(falas.length)]; }

    // Depois da animação de revelação, vejo se alguém já conseguiu duas vitórias.
    // Se ninguém chegou lá, começa outra rodada; se o jogador perdeu, registro o fim.
    private void avancar() {
        if (fase != Fase.REVELACAO || cena.animacao.ocupada()) return;
        escolha = casa = -1;
        if (vitorias >= 2) {
            fase = Fase.PORTA; pergunta = 0;
            cena.animacao.abrirPorta();
            mensagem = "Minhas velas se apagaram. A porta está aberta... venha à minha sala de perguntas.";
        } else if (rodada-vitorias >= 2) terminar(false, "Suas duas velas se apagaram. Hoje, o duelo foi meu. Quer tentar de novo? Eu espero.");
        else { fase = Fase.DUELO; mensagem = comentario("Sua vez. Olhe suas cartas... qual delas vai me surpreender?", "Ainda temos fogo na mesa. Escolha sua próxima carta."); }
        atualizarTela();
    }

    // A porta termina de abrir antes de eu trocar a fase e mostrar a primeira pergunta.
    private void entrarQuiz() {
        if (fase != Fase.PORTA) return;
        fase = Fase.JAVA;
        naSalaQuiz=true;
        respostaPendente=false;
        embaralhar();
        mensagem = enunciarPergunta();
        atualizarTela();
    }

    // Esses métodos pequenos evitam repetir a mesma comparação em vários lugares.
    private boolean quiz() { return fase == Fase.JAVA || fase == Fase.CURIOSIDADES; }
    private boolean javaQuiz() { return fase == Fase.JAVA; }

    private String[][] bancoAtual() { return javaQuiz() ? JAVA : CURIOSIDADES; }

    // Pego a pergunta do banco que está valendo e junto com o número dela.
    private String enunciarPergunta() {
        String[][] banco=bancoAtual();
        return "Pergunta " + (pergunta+1) + " de " + banco.length + ". " + banco[pergunta][0];
    }

    // 'opcao' é o botão apertado. A tabela ordem me diz qual resposta original
    // aquele botão recebeu; quando o valor é 0, caiu ali a primeira (que é a certa).
    private void responder(int opcao) {
        if (!quiz() || respostaPendente) return;
        String[][] banco = bancoAtual();
        if (ordem[opcao] == 0) {
            pontos += 15;
            if (fase == Fase.JAVA) pontosJava += 15;
            mensagem = comentario("Exatamente. Você conhece a resposta... anote mais 15 pontos.",
                                 "Correto. Não foi só sorte, então... mais 15 pontos para você.");
        } else mensagem = "Desta vez, não. A resposta era " + banco[pergunta][1] + ". Guarde isso... o arquivo gosta de quem aprende.";
        respostaPendente=true;
        atualizarTela();
    }

    // Aqui decido se mostro outra pergunta, se libero o quiz de curiosidades ou
    // se termino o jogo. São 5 perguntas valendo 15; 60 pontos significa acertar 4.
    private void avancarQuiz() {
        if (!quiz() || !respostaPendente) return;
        respostaPendente=false;
        pergunta++;
        if (pergunta < bancoAtual().length) {
            embaralhar();
            mensagem = enunciarPergunta();
        } else if (javaQuiz() && pontosJava >= 60) {
            fase = Fase.CURIOSIDADES; pergunta = 0; embaralhar();
            mensagem = "Você passou pela porta de Java. Agora, a última prova. " + enunciarPergunta();
        } else if (javaQuiz()) {
            terminar(false, "Você reuniu " + pontosJava + " pontos em Java. Esta porta exige 60. Volte à mesa... uma nova tentativa pode mudar tudo.");
        } else {
            terminar(true, "Você atravessou as três portas. Admito: jogou bem. Seu nome e seus pontos ficam no arquivo.");
        }
        atualizarTela();
    }

    // Embaralho só os índices, não mexo no texto das perguntas. Assim continuo
    // sabendo qual alternativa era a certa mesmo depois de trocar os botões de lugar.
    // É o embaralhamento de Fisher-Yates: vou trocando cada posição por outra aleatória.
    private void embaralhar() {
        ordem = new int[] {0, 1, 2};
        for (int i = 2; i > 0; i--) {
            int j = acaso.nextInt(i + 1), t = ordem[i]; ordem[i] = ordem[j]; ordem[j] = t;
        }
    }

    // Registro o resultado e organizo o ranking do maior número de pontos para o menor.
    private void terminar(boolean ganhou, String texto) {
        cena.animacao.revelar(ganhou ? 2 : 0, ganhou ? 0 : 2);
        fase = Fase.RESULTADO;
        mensagem = texto;
        ranking.add(new Resultado(jogador, pontos));
        ranking.sort((a, b) -> Integer.compare(b.pontos, a.pontos));
    }
    // ===== 2. JANELA E CONTROLES =============================================
    // A janela principal é um JFrame. Deixei a cena num painel separado dos botões,
    // porque a imagem precisa ser redesenhada várias vezes durante as animações.
    private final Cena3D cena = new Cena3D();
    private final Fala fala = new Fala();
    private final JPanel controles = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
    private final JTextField nomeCampo = new JTextField("Viajante", 16);
    private final Color branco = cinza(232), fundo = cinza(11);

    // Aqui monto a janela, ajusto o tamanho e coloco a cena e a faixa de botões.
    public JogoCartas() {
        super("O Arquivo das Sombras");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(JogoCartas.this, "Fechar o jogo?", "Sair",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) dispose();
            }
        });
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(fundo);
        raiz.add(cena, BorderLayout.CENTER);
        JPanel base = new JPanel(new BorderLayout()); base.setBackground(cinza(8));
        base.setBorder(BorderFactory.createMatteBorder(1,0,0,0,cinza(63)));
        controles.setBackground(fundo); base.add(controles, BorderLayout.CENTER);
        raiz.add(base, BorderLayout.SOUTH);
        setContentPane(raiz);
        setSize(1060, 700); setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);
        nomeCampo.addActionListener(e -> { if (fase == Fase.INICIO) iniciar(); });
        atualizarTela();
    }

    // Faço os botões por aqui para todos terem a mesma aparência. O Runnable é
    // a ação que cada botão executa quando alguém clica nele.
    private JButton botao(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Monospaced", Font.BOLD, 13));
        b.setForeground(branco); b.setBackground(cinza(20));
        b.setFocusPainted(false); b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(cinza(83)), new EmptyBorder(8, 15, 8, 15)));
        b.addActionListener(e -> acao.run());
        controles.add(b); return b;
    }

    // Em vez de esconder e mostrar cada botão separadamente, eu limpo a faixa e
    // monto de novo os controles que combinam com a fase atual do jogo.
    private void atualizarTela() {
        controles.removeAll();
        fala.dizer(mensagem);

        if (fase == Fase.INICIO) {
            JLabel convite = new JLabel("SEU NOME"); convite.setForeground(branco);
            controles.add(convite); controles.add(nomeCampo);
            botao("ABRIR O ARQUIVO", () -> iniciar());
        } else if (fase == Fase.DUELO) {
            for (int i = 0; i < 3; i++) {
                final int n = i;
                JButton b = botao((escolha == i ? "◆ " : "") + CARTAS[i], () -> selecionar(n));
                if (escolha == i) b.setBackground(cinza(85));
            }
            JButton confirmar = botao("REVELAR CARTAS", () -> jogar());
            confirmar.setEnabled(escolha >= 0);
        } else if (fase == Fase.REVELACAO) {
            botao(cena.animacao.ocupada() ? "REVELANDO..." : "CONTINUAR", () -> avancar())
                .setEnabled(!cena.animacao.ocupada());
        } else if (fase == Fase.PORTA) {
            botao("A PORTA ESTÁ SE ABRINDO...", () -> {}).setEnabled(false);
        } else if (quiz() && !respostaPendente) {
            String[][] banco = bancoAtual();
            for (int i = 0; i < 3; i++) {
                final int n = i;
                botao((char)('A' + i) + "  " + banco[pergunta][1 + ordem[i]], () -> responder(n));
            }
        } else if (quiz() && respostaPendente) {
            botao("PRÓXIMA PERGUNTA", () -> avancarQuiz());
        } else {
            botao("JOGAR NOVAMENTE", () -> {
                fase = Fase.INICIO; mensagem = "Voltou para outra partida? Diga seu nome. Vou reacender as velas."; atualizarTela();
            });
        }
        controles.revalidate(); controles.repaint(); cena.repaint();
    }

    // O nome interno da fase é maior; aqui escolho o texto curto que aparece na tela.
    private String tituloFase() {
        switch (fase) {
            case INICIO: return "ENTRADA";
            case DUELO: case REVELACAO: return "I / DUELO";
            case PORTA: return "PASSAGEM";
            case JAVA: return "II / JAVA";
            case CURIOSIDADES: return "III / CURIOSIDADES";
            default: return "REGISTRO FINAL";
        }
    }
    // Este painel junta a imagem 3D com os textos e também recebe os cliques nas cartas.
    private class Cena3D extends JPanel {
        private final Mesa3D mesa = new Mesa3D();
        private final Animacao animacao = new Animacao();
        private long ultimoQuadro;
        // 33 ms dá mais ou menos 30 atualizações por segundo. O Timer do Swing
        // anima sem usar Thread.sleep(), que poderia deixar a janela travada.
        private final javax.swing.Timer relogio = new javax.swing.Timer(33, e -> animar());
        // Quando o painel entra na janela, começo o Timer. Quando sai, paro para
        // não ficar atualizando uma cena que nem está aparecendo.
        @Override public void addNotify() {
            super.addNotify(); ultimoQuadro=System.nanoTime(); relogio.start();
        }
        @Override public void removeNotify() { relogio.stop(); super.removeNotify(); }
        // Calculo o tempo que passou desde o quadro anterior para a animação não
        // depender tanto da velocidade do computador. Também atualizo o texto da fala.
        private void animar() {
            long agora=System.nanoTime();
            double dt=Math.min(.1,(agora-ultimoQuadro)/1e9); ultimoQuadro=agora;
            boolean estavaOcupada=animacao.ocupada();
            animacao.atualizar(dt);
            fala.atualizar(dt);
            animacao.falando=fala.falando();
            if(estavaOcupada && !animacao.ocupada()) atualizarTela();
            if(fase == Fase.PORTA && !animacao.portaAtiva()) entrarQuiz();
            repaint();
        }
        private Rectangle area = new Rectangle();
        private Rectangle faixaFala = new Rectangle();
        Cena3D() {
            setBackground(Color.BLACK);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (faixaFala.contains(e.getPoint())) { fala.completar(); repaint(); return; }
                    if (area.width == 0 || !area.contains(e.getPoint())) return;
                    // A cena interna tem 640x360, mas na janela ela pode ficar maior.
                    // Converto o clique de volta para a resolução usada pelo renderizador.
                    int x = (e.getX() - area.x) * 640 / area.width;
                    int y = (e.getY() - area.y) * 360 / area.height;
                    for (int i = 2; i >= 0; i--)
                        if (mesa.mao[i] != null && mesa.mao[i].contains(x, y)) { selecionar(i); break; }
                }
            });
        }

        // O Swing chama este método quando precisa redesenhar o painel.
        @Override protected void paintComponent(Graphics original) {
            super.paintComponent(original);
            mesa.desenhar(escolha, fase == Fase.REVELACAO ? casa : -1,
                         animacao, naSalaQuiz, fase == Fase.PORTA);
            Graphics2D g = (Graphics2D)original.create();
            // Mantenho a proporção 16:9 para a imagem não ficar esticada.
            double escala = Math.min(getWidth() / 640.0, getHeight() / 360.0);
            int largura = (int)(640 * escala), altura = (int)(360 * escala);
            area.setBounds((getWidth()-largura)/2, (getHeight()-altura)/2, largura, altura);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(mesa.render.imagem, area.x, area.y, largura, altura, null);
            desenharInterface(g);
            if(fase != Fase.PORTA || animacao.portaTempo<1.2)
                faixaFala = fala.desenhar(g, getWidth(), 53, animacao.total);
            else faixaFala.setBounds(0,0,0,0);
            g.dispose();
        }

        // Esses textos são desenhados por cima da cena 3D, como um HUD de jogo.
        private void desenharInterface(Graphics2D g) {
            int w = getWidth(), h = getHeight();
            g.setFont(new Font("Monospaced",Font.BOLD,12));
            g.setColor(new Color(0,0,0,180));
            g.fillRect(12,11,214,25);
            g.fillRect(w-214,11,202,25);
            g.setColor(cinza(210));
            g.drawString(tituloFase(),21,28);
            String placar=String.format("%s  /  %03d",jogador.toUpperCase(Locale.ROOT),pontos);
            g.drawString(placar,w-21-g.getFontMetrics().stringWidth(placar),28);
            g.setColor(cinza(150));
            g.drawLine(12,38,45,38);g.drawLine(w-45,38,w-12,38);
            if(fase==Fase.DUELO || fase==Fase.REVELACAO) {
                g.setFont(new Font("Monospaced",Font.BOLD,11));
                g.setColor(new Color(0,0,0,175));g.fillRect(12,h-42,225,27);
                g.setColor(cinza(210));
                g.drawString("CHAMAS  "+(2-(rodada-vitorias))+" : "+(2-vitorias),21,h-24);
                if(fase==Fase.REVELACAO && escolha>=0 && casa>=0) {
                    String confronto=CARTAS[escolha]+"  /  "+CARTAS[casa];
                    int largura=g.getFontMetrics().stringWidth(confronto);
                    g.setColor(new Color(0,0,0,175));g.fillRect(w-largura-40,h-42,largura+28,27);
                    g.setColor(cinza(210));g.drawString(confronto,w-largura-26,h-24);
                }
            } else if(quiz()) {
                String progresso=String.format("PERGUNTA %02d / %02d",pergunta+1,bancoAtual().length);
                g.setColor(new Color(0,0,0,175));g.fillRect(12,h-42,180,27);
                g.setColor(cinza(210));g.drawString(progresso,21,h-24);
            } else if(fase==Fase.RESULTADO) painelRanking(g,w,h);
        }

        // O ranking é só da sessão atual e mostro aqui as posições que cabem na janela.
        private void painelRanking(Graphics2D g, int w, int h) {
            int x = w / 2 - 215, y = 92;
            g.setColor(new Color(4, 4, 4, 225)); g.fillRect(x, y, 430, Math.min(h - y - 24, 55 + ranking.size() * 28));
            g.setColor(cinza(205));
            g.setFont(new Font("Monospaced", Font.BOLD, 17));
            g.drawString("RANKING DA SESSÃO", x + 18, y + 30);
            g.setFont(new Font("Monospaced", Font.PLAIN, 14));
            for (int i = 0; i < ranking.size() && y + 57 + i * 28 < h - 35; i++) {
                Resultado r = ranking.get(i);
                g.drawString(String.format("%02d  %-20s %4d", i + 1, r.nome, r.pontos), x + 18, y + 57 + i * 28);
            }
        }
    }

    // O Java começa a execução pelo main. Uso invokeLater porque a janela Swing
    // deve ser criada na fila de eventos da própria interface.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JogoCartas().setVisible(true));
    }
}
