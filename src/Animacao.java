import java.util.*;

/**
 * Guardei aqui os valores que mudam com o tempo: movimento da câmera, balança,
 * velas e passagem pela porta. As regras de quem ganhou continuam em JogoCartas.
 */
class Animacao {
        // Duração da revelação de cada rodada do duelo.
        static final double DURACAO = 2.4;
        double tempo = DURACAO, total, inclinacao, velocidade;
        boolean falando;
        // Tempo total reservado para a câmera sair da mesa e chegar à outra sala.
        static final double CAMINHADA = 8.1;
        double portaTempo = CAMINHADA;
        boolean atravessouPorta;
        int vitorias, derrotas;
        final double[] chamas = {1,1,1,1}; // duas suas, duas da casa
        // Volto tudo ao estado inicial quando o jogador começa outra partida.
        void reiniciar() {
            tempo=DURACAO; total=0; inclinacao=velocidade=0;
            vitorias=derrotas=0; portaTempo=CAMINHADA; atravessouPorta=false; Arrays.fill(chamas,1);
        }
        // Recebo o resultado do duelo para animar as velas e a balança.
        void revelar(int vitorias, int derrotas) {
            this.vitorias=vitorias; this.derrotas=derrotas; tempo=0;
        }
        // Enquanto esse tempo não acaba, ainda considero a revelação em andamento.
        boolean ocupada() { return tempo<DURACAO; }
        void abrirPorta() { atravessouPorta=true; portaTempo=0; }
        boolean portaAtiva() { return portaTempo<CAMINHADA; }
        // Depois desse ponto da caminhada, a câmera já chegou à sala do quiz.
        boolean dentroSala() { return portaTempo>=6.1; }
        double aparicao() { return suave((portaTempo-6.65)/1.25); }
        double abertura() { return suave((portaTempo-3.8)/1.25); }
        // Cada visão guarda posição da câmera (x,y,z), ponto para onde ela olha
        // (x,y,z) e abertura da lente. São os enquadramentos usados no duelo.
        static final double[] MAO = {0,3,-6, 0,0,3, 60};
        static final double[] TABULEIRO = {0,4.2,-3.8, 0,0,2, 55};
        static final double[] BALANCA = {-.6,3.2,-4.1, -2.3,.75,1.7, 55};
        static double suave(double t) {
            t=Math.max(0,Math.min(1,t));
            return t*t*t*(t*(t*6-15)+10); // velocidade e aceleração zeram nas extremidades
        }
        // A câmera contorna a mesa pelo corredor direito. A ordem destes pontos
        // acompanha os tempos logo abaixo, até chegar na outra sala.
        static final double[][] CAMINHO = {
            MAO,
            {0,3.45,-5.7, 1.2,1,7, 62},
            {5.5,3.3,-4.6, 5.3,1.4,4, 62},
            {5.5,3.15,4.8, 2.8,1.4,10.5, 60},
            {5.5,3.15,7.9, 0,1.4,11.5, 59},
            {1.5,3.1,9.1, 0,1.3,11.5, 58},
            {0,2.8,11.4, 0,1.3,17.5, 58},
            {0,2.65,12.35, 0,1.2,18.5, 58}
        };
        static final double[] TEMPOS = {0,.8,2.0,3.8,4.4,5.2,6.1,6.65};

        // Escolho a posição da câmera de acordo com o momento da animação.
        // Durante a passagem, ela segue os pontos do corredor; no duelo, alterna
        // entre a visão das cartas, do tabuleiro e da balança.
        double[] camera() {
            if(portaAtiva()) {
                double[] vista=CAMINHO[CAMINHO.length-1].clone();
                for(int i=0;i<TEMPOS.length-1;i++) if(portaTempo<TEMPOS[i+1]) {
                    double t=(portaTempo-TEMPOS[i])/(TEMPOS[i+1]-TEMPOS[i]);
                    for(int eixo=0;eixo<vista.length;eixo++) vista[eixo]=trajeto(i,eixo,t);
                    break;
                }
                if(portaTempo>.8 && portaTempo<6.4)
                    vista[1]+=.055*Math.sin(portaTempo*15)*Math.sin(Math.PI*suave((portaTempo-.8)/5.6));
                return vista;
            }
            if(atravessouPorta) return CAMINHO[CAMINHO.length-1].clone();
            if(tempo<.45) return misturar(MAO,TABULEIRO,suave(tempo/.45));
            if(tempo<.85) return TABULEIRO.clone();
            if(tempo<1.25) return misturar(TABULEIRO,BALANCA,suave((tempo-.85)/.4));
            if(tempo<1.8) return BALANCA.clone();
            return misturar(BALANCA,MAO,suave((tempo-1.8)/.6));
        }
        // Interpola dois pontos de câmera: começo e fim de cada trecho.
        // A curva suave evita mudanças bruscas na caminhada.
        static double trajeto(int trecho, int eixo, double t) {
            double a = CAMINHO[trecho][eixo];
            double b = CAMINHO[trecho+1][eixo];
            return a + (b-a)*suave(t);
        }
        // A mão do jogador abaixa durante a cena da porta e volta a aparecer no duelo.
        double baixarMao() {
            if(tempo<.45) return suave(tempo/.45);
            if(tempo<1.8) return 1;
            return 1-suave((tempo-1.8)/.6);
        }
        // Misturo dois enquadramentos aos poucos. Sem isso a câmera daria um salto seco.
        static double[] misturar(double[] a,double[] b,double t) {
            double[] r=new double[a.length];
            for(int i=0;i<r.length;i++) r[i]=a[i]+(b[i]-a[i])*t;
            return r;
        }
        // Esta função roda várias vezes por segundo. Uso dt (tempo decorrido) para
        // os movimentos continuarem parecidos mesmo se o computador variar o FPS.
        void atualizar(double dt) {
            total+=dt; tempo=Math.min(DURACAO,tempo+dt);
            portaTempo=Math.min(CAMINHADA,portaTempo+dt);
            // Balança: inclina para o lado de quem venceu mais rodadas.
            double alvo = tempo<1.05 ? inclinacao : (vitorias-derrotas)*.20;
            velocidade += (36*(alvo-inclinacao)-9*velocidade)*dt;
            inclinacao += velocidade*dt;
            for(int i=0;i<4;i++) {
                int perdidas=i<2 ? derrotas : vitorias;
                double alvoChama=tempo<1.25 ? chamas[i] : ((i%2)<2-perdidas ? 1 : 0);
                chamas[i]+=(alvoChama-chamas[i])*(1-Math.exp(-5*dt));
            }
        }
    }
