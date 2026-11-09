import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

/**
 * Aqui monto o cenário inteiro: sala, mesa, cartas, velas, porta e o oponente.
 * Trabalho com coordenadas 3D e mando as formas para o Render3D desenhar.
 */
class Mesa3D {
        // A cena nasce em 640x360 e depois é ampliada para caber na janela.
        final Render3D render = new Render3D(640, 360);
        final BufferedImage[] cartas = Texturas.criarCartas();
        final BufferedImage costas = Texturas.costas(), madeira = Texturas.madeira(),
                            rostoJava = Texturas.rostoJava(), livro = Texturas.livro();
        final BufferedImage[] facesJava = Texturas.facesJava();
        // Além de desenhar as cartas na mão, guardo seus polígonos para detectar cliques.
        final Polygon[] mao = new Polygon[3];
        double maoBaixa;

        // Este é o método que monta um quadro completo. Primeiro ajusto a câmera,
        // depois desenho os ambientes e objetos, e no final coloco as cartas da mão.
        void desenhar(int selecionada, int revelada, Animacao animacao, boolean quiz, boolean passagem) {
            render.configurarCamera(animacao.camera());
            render.chamas[0]=Math.max(animacao.chamas[0],animacao.chamas[1]);
            render.chamas[1]=Math.max(animacao.chamas[2],animacao.chamas[3]);
            boolean salaQuiz=quiz || passagem && animacao.dentroSala();
            maoBaixa=salaQuiz ? 1 : passagem ? Animacao.suave(animacao.portaTempo/.55) : animacao.baixarMao();
            render.limpar();
            // Um único espaço: os móveis continuam visíveis através da porta.
            sala(passagem || quiz ? animacao.abertura() : 0);
            salaQuiz();
            mesa();
            mesaQuiz();
            render.caixa(0,.12,17.35,1.5,.12,1.8,76);
            plana(0,17.35,2.7,3.4,livro,.25);
            double presenca=1;
            if(passagem) presenca=salaQuiz ? .15+.85*animacao.aparicao()
                                          : 1-Animacao.suave(animacao.portaTempo/1.2);
            render.deslocamentoZ=salaQuiz ? 14 : 0;
            render.deslocamentoY=salaQuiz && passagem ? -2.3*(1-animacao.aparicao()) : 0;
            render.luminosidade=presenca;
            if(presenca>.02 && (!salaQuiz || !passagem || animacao.aparicao()>.04))
                adversario(animacao.falando,animacao.total);
            render.deslocamentoZ=render.deslocamentoY=0;
            render.luminosidade=1;
            if(salaQuiz && passagem) materializacao(animacao.aparicao(),animacao.total);
            for(int i=0;i<2;i++) {
                vela(-3.45+i*.48,4.4,1.20+i*.25,animacao.chamas[i],animacao.total+i);
                vela(2.95+i*.48,4.4,1.20+i*.25,animacao.chamas[2+i],animacao.total+i+2);
            }
            tabuleiro(selecionada,revelada);
            balanca(animacao);
            if(!salaQuiz && !passagem) for (int i=0; i<3; i++) cartaNaMao(i, selecionada == i);
            else Arrays.fill(mao,null);
            Graphics2D g=render.imagem.createGraphics();
            g.setFont(new Font("Monospaced",Font.BOLD,10));
            g.setColor(JogoCartas.cinza(205));
            if(!salaQuiz && !passagem) {
                rotulo(g,"VOCÊ",-3.2,.12,3.8);
                rotulo(g,"CASA",3.2,.12,3.8);
            }
            g.dispose();
        }
        // Espaços das cartas no centro da mesa. A carta da casa só aparece virada
        // quando já existe uma revelação; antes disso mostro apenas o verso.
        void tabuleiro(int selecionada,int revelada) {
            for (int fila=0; fila<2; fila++) for (int i=0; i<3; i++) {
                double x = (i-1)*1.40, z=.9+fila*2.35;
                plana(x, z, 1.23, 1.95, costas, .015);
            }
            plana(0, 3.25, 1.23, 1.95, revelada < 0 ? costas : cartas[revelada], .03);
            if (selecionada >= 0) plana(0, .9, 1.23, 1.95, cartas[selecionada], .035);
            // Pilha de compra à direita, com espessura visível.
            render.caixa(2.9, .11, .5, .55, .11, .8, 92);
            plana(2.9, .5, 1.1, 1.6, costas, .23);
        }
        // A balança é feita de caixas e um quadrilátero. Os pesos de cada lado
        // representam as vitórias e derrotas que a classe Animacao recebeu.
        void balanca(Animacao animacao) {
            // Balança à esquerda, fora das casas do tabuleiro.
            render.caixa(-3.05, .1, 1.6, .45, .1, .33, 72); render.caixa(-3.05, .85, 1.6, .045, .75, .045, 140);
            double dx=.70*Math.cos(animacao.inclinacao), dy=.70*Math.sin(animacao.inclinacao);
            render.quad(-3.05-dx,1.55-dy-.04,1.56,-3.05+dx,1.55+dy-.04,1.56, -3.05+dx,1.55+dy+.04,1.56,-3.05-dx,1.55-dy+.04,1.56,175);
            for (int i=-1; i<=1; i+=2) {
                double x=-3.05+i*dx, y=1.55+i*dy;
                render.caixa(x,y-.33,1.6,.015,.33,.015,111); render.caixa(x,y-.66,1.6,.27,.035,.22,150);
                int pesos=i<0 ? animacao.vitorias : animacao.derrotas;
                for(int n=0;n<pesos;n++) render.caixa(x,y-.59+n*.11,1.6,.09,.05,.09,210);
            }
        }
        // Os rótulos são textos 2D colocados na posição em que o ponto 3D aparece.
        void rotulo(Graphics2D g,String texto,double x,double y,double z) {
            Render3D.V p=render.p(x,y,z,0,0);
            if(p.z<=.12) return;
            g.drawString(texto,(int)p.x-g.getFontMetrics().stringWidth(texto)/2,(int)p.y);
        }

        // A primeira sala é o corredor do duelo. A abertura controla as duas folhas da porta.
        void sala(double abertura) {
            paredeLateral(-8,-7,11,32);
            paredeLateral(8,-7,11,32);
            render.quad(-8,-1,-7,8,-1,-7,8,-1,11,-8,-1,11,24);
            for(int i=-5;i<=5;i++) {
                if(Math.abs(i*1.45)>2.25) render.caixa(i*1.45,1.3,10.9,.055,3.6,.08,27);
                if(Math.abs(i*1.45+.5)>2.25) render.caixa(i*1.45+.5,2,10.95,.012,2.8,.02,22);
            }
            for(int lado=-1;lado<=1;lado+=2) {
                render.caixa(lado*7.45,1,6.9,.18,3.4,.22,35); render.caixa(lado*7.45,4.65,4.5,.24,.12,3.2,28);
                for(int i=0;i<4;i++) {
                    render.caixa(lado*7.15,-.15,1.0+i*2.2,.28,1.8,.28,38+i*5);
                    render.caixa(lado*7.15,1.85,1.0+i*2.2,.44,.11,.44,75);
                }
                // Armários e pequenas luzes tornam o corredor reconhecível ao caminhar.
                for(int i=0;i<3;i++) {
                    render.caixa(lado*7.05,-.22,3.0+i*2.0,.35,.75,.65,47); render.caixa(lado*7.05,.55,3.0+i*2.0,.38,.04,.68,115);
                }
            }
            for(int z=-4;z<=8;z+=3) {
                render.caixa(0,4.7,z,6.9,.11,.11,37);
                for(int lado=-1;lado<=1;lado+=2)
                    render.caixa(lado*6.8,2.0,z,.11,2.8,.11,42);
            }
            render.caixa(0,4.8,10.5,7.5,.13,.25,31);
            piso(-8,8,-7,11);
            sombra(0,1.6,5.1,5.7);
            sombra(0,8.5,1.3,.9);
            for(int lado=-1;lado<=1;lado+=2) {
                for(int z=0;z<=8;z+=2) sombra(lado*7.0,z,.55,.8);
                // Frisos e painéis dão escala à sala sem acrescentar texturas externas.
                for(int z=-4;z<=7;z+=3) {
                    render.caixa(lado*7.84,2.0,z,.025,1.45,.9,49); render.caixa(lado*7.79,3.49,z,.035,.05,.96,83);
                }
            }
            // Faixa de pedra no corredor: a passagem é legível mesmo sentado.
            render.quad(4.7,-.975,-5.3,6.35,-.975,-5.3,6.35,-.975,8.6,4.7,-.975,8.6,47);
            render.quad(-1.8,-.974,9,4.7,-.974,9,4.7,-.974,10.9,-1.8,-.974,10.9,43);
            porta(abertura);
        }
        // Faço o piso em vários quadriláteros pequenos para formar as placas do chão.
        void piso(double esquerda,double direita,double inicio,double fim) {
            for(double z=inicio;z<fim;z+=2) for(double x=esquerda;x<direita;x+=2) {
                double x2=Math.min(direita,x+1.97),z2=Math.min(fim,z+1.97);
                int tom=29+Math.floorMod((int)(x*7+z*11),7);
                render.quad(x,-.99,z,x2,-.99,z,x2,-.99,z2,x,-.99,z2,tom);
            }
        }

        // Segunda sala, onde acontece o quiz. Mantive o mesmo sistema de coordenadas
        // para a câmera conseguir atravessar a porta sem trocar de cenário de uma vez.
        void salaQuiz() {
            paredeFundo(25,-7,7,41);
            paredeLateral(-7,11,25,38);
            paredeLateral(7,11,25,38);
            render.quad(-7,-1,11,7,-1,11,7,-1,25,-7,-1,25,42);
            piso(-7,7,11,25);
            sombra(0,17.5,4.5,4.7);
            for(int lado=-1;lado<=1;lado+=2) {
                render.caixa(lado*6.75,3.8,18,.09,.07,6.1,82); render.caixa(lado*6.75,.15,18,.09,.07,6.1,48);
            }
            for(int lado=-1;lado<=1;lado+=2) {
                for(int z=14;z<=22;z+=3) {
                    render.caixa(lado*5.6,.7,z,.38,2.0,.39,66); render.caixa(lado*5.6,2.7,z,.53,.12,.53,111);
                }
                for(int nivel=0;nivel<3;nivel++) {
                    render.caixa(lado*4, nivel*.92,22,.95,.08,.65,97);
                    for(int i=0;i<5;i++)
                        render.caixa(lado*(3.35+i*.3), nivel*.92+.4,22,.12,.38,.40,58+i*12);
                }
                vela(lado*4.5,19,1.6,1,0);
            }
            for(int i=-4;i<=4;i++)
                render.caixa(i*1.35,1.6,24.8,.08,3.2,.10,64);
            render.caixa(0,3.7,24.55,2.2,.11,.15,128); render.caixa(0,1.9,24.5,1.2,1.6,.12,19);
            render.quadTextura(-.92,.55,24.32,.92,.55,24.32, .92,3.15,24.32,-.92,3.15,24.32,rostoJava);
        }

        // A mesa e os bancos da sala de perguntas ficam separados da mesa do duelo.
        void mesaQuiz() {
            for(int lado=-1;lado<=1;lado+=2) for(int ponta=0;ponta<2;ponta++)
                render.caixa(lado*3.7,-.61,13.9+ponta*7.3,.2,.39,.2,65);
            render.caixa(0,-.16,17.5,4.25,.18,4.45,36);
            render.quadTextura(-4.2,.03,13,4.2,.03,13, 4.2,.03,22,-4.2,.03,22,madeira);
            for(int i=-3;i<=3;i++)
                render.caixa(i*1.05,.035,17.5,.015,.015,4.4,17);
            render.caixa(0,-.42,13.2,4.3,.16,.07,96);
        }
        // Para não desenhar cada parede inteira de uma vez, monto as laterais e o
        // fundo com vários quadriláteros menores. É simples, mas resolve para a cena.
        void paredeLateral(double x,double inicio,double fim,int tom) {
            for(double z=inicio;z<fim;z+=2) for(double y=-1;y<6;y+=1.75)
                render.quad(x,y,z,x,y,Math.min(fim,z+2), x,Math.min(6,y+1.75),Math.min(fim,z+2), x,Math.min(6,y+1.75),z,tom);
        }
        void paredeFundo(double z,double esquerda,double direita,int tom) {
            for(double x=esquerda;x<direita;x+=2) for(double y=-1;y<6;y+=1.75)
                render.quad(x,y,z,Math.min(direita,x+2),y,z, Math.min(direita,x+2),Math.min(6,y+1.75),z, x,Math.min(6,y+1.75),z,tom);
        }

        // Partículas simples aparecem durante a chegada na sala, dando a impressão
        // de que o personagem está se formando no lugar, em vez de surgir do nada.
        void materializacao(double progresso,double tempo) {
            if(progresso<=0||progresso>=.99) return;
            for(int i=0;i<22;i++) {
                double ang=i*2.399+tempo*.7, raio=.35+(.8*(i%5)/5.0);
                double x=Math.sin(ang)*raio, z=22.2+Math.cos(ang)*raio;
                double y=.15+((i*7)%23)/10.0+progresso*1.1;
                render.caixa(x,y,z,.025,.07,.025,80+(i%4)*35);
            }
        }

        // Desenho o vão e as duas folhas da porta. Cada folha gira para dentro
        // conforme abertura vai de 0 até o valor final.
        void porta(double abertura) {
            final double parede=11, metade=2.2, topo=4.35, chao=-1;
            render.quad(-8,chao,parede,-metade,chao,parede,-metade,6,parede,-8,6,parede,25);
            render.quad(metade,chao,parede,8,chao,parede,8,6,parede,metade,6,parede,25);
            render.quad(-metade,topo,parede,metade,topo,parede,metade,6,parede,-metade,6,parede,25);
            render.caixa(0,-1,11,metade,.025,.32,87);
            double angulo=abertura*1.50;
            for(int lado=-1;lado<=1;lado+=2) {
                double dobradica=lado*metade;
                double ponta=dobradica-lado*metade*Math.cos(angulo);
                double z=parede+metade*Math.sin(angulo);
                render.quad(dobradica,chao,parede,ponta,chao,z,ponta,topo, z,dobradica,topo,parede,69);
                // Friso: segue a mesma rotação da porta.
                render.quad(dobradica,.25,parede-.02,ponta,.25,z-.02, ponta,.32,z-.02,dobradica,.32,parede-.02,122);
                render.quad(dobradica,3.45,parede-.02,ponta,3.45,z-.02, ponta,3.52,z-.02,dobradica,3.52,parede-.02,122);
                render.caixa(dobradica,1.7,parede-.08,.10,2.75,.16,104);
            }
            render.caixa(0,topo+.08,parede-.08,metade+.18,.13,.16,130);
        }

        // Mesa principal do duelo, com tampo texturizado e pés.
        void mesa() {
            for(int lado=-1;lado<=1;lado+=2) for(int ponta=0;ponta<2;ponta++)
                render.caixa(lado*3.8,-.65,-2.8+ponta*8.8,.18,.35,.18,46);
            render.caixa(0,-.20,1.65,4.3,.20,5.35,25);
            render.quadTextura(-4.3,.003,-3.7,4.3,.003,-3.7, 4.3,.003,7,-4.3,.003,7,madeira);
            for(int i=-3;i<=3;i++)
                render.quad(i*1.1, .008,-3.7,i*1.1+.018,.008,-3.7, i*1.1+.018,.008,7,i*1.1,.008,7,18);
        }
        // Não é uma sombra real calculada pela luz; são três camadas no chão
        // que dão uma ideia de sombra embaixo dos móveis.
        void sombra(double x,double z,double rx,double rz) {
            // Três camadas no piso simulam penumbra sob móveis e colunas.
            for(int i=2;i>=0;i--) {
                double escala=1+i*.17, y=-.965-i*.002;
                int tom=12+i*5;
                render.quad(x-rx*escala,y,z-rz*escala,x+rx*escala,y,z-rz*escala, x+rx*escala,y,z+rz*escala,x-rx*escala,y,z+rz*escala,tom);
            }
        }

        // O oponente foi montado com formas simples e uma tela na cabeça.
        // Quando ele está falando, alterno os quadros do rosto para simular movimento.
        void adversario(boolean falando,double tempo) {
            // O Deus Java é o próprio oponente sentado à mesa.
            render.caixa(0,.85,8.45,1.05,.95,.40,12); render.caixa(-1.12,.95,8.2,.24,.45,.34,19);
            render.caixa(1.12,.95,8.2,.24,.45,.34,19);
            render.caixa(0,1.65,8.10,.35,.24,.28,38); // pescoço
            // Carcaça de monitor CRT: o símbolo na tela é o único ponto de cor.
            render.caixa(0,2.58,7.97,.88,.77,.29,70); render.caixa(0,2.58,7.66,.76,.64,.025,115);
            BufferedImage face=facesJava[falando ? 1+(int)(tempo*12)%3 : 0];
            render.quadTextura(-.72,1.98,7.62,.72,1.98,7.62, .72,3.18,7.62,-.72,3.18,7.62,face);
            render.caixa(0,1.86,7.65,.39,.045,.04,39);
        }

        // A chama muda de tamanho e treme um pouco. Quando está apagando, também
        // desenho uma fumacinha para a transição não ficar tão seca.
        void vela(double x,double z,double altura,double chama,double tempo) {
            render.caixa(x,.08,z,.3,.08,.26,65); render.caixa(x,altura/2,z,.085,altura/2,.085,165);
            render.caixa(x,altura+.025,z,.012,.03,.012,25);
            if(chama>.02) {
                double tremor=Math.sin(tempo*11)*.015;
                double tamanho=chama*(.30+.04*Math.sin(tempo*17));
                render.quad(x-.07*chama,altura+.05,z,x+.07*chama,altura+.05,z,
                            x+tremor+.01,altura+.05+tamanho,z,x+tremor-.01,altura+.05+tamanho,z,241);
            }
            if(chama<.95 && chama>.025) {
                double subida=(1-chama)*.55;
                render.caixa(x+Math.sin(tempo*4)*.04,altura+.15+subida,z,.025,.09,.02,60);
            }
        }

        // Atalho para desenhar uma imagem deitada sobre o chão ou sobre uma mesa.
        void plana(double x,double z,double largura,double altura,BufferedImage tex,double y) {
                render.quadTextura(x-largura/2,y,z-altura/2,x+largura/2,y,z-altura/2, x+largura/2,y,z+altura/2,x-largura/2,y,z+altura/2,tex);
        }

        // Estas cartas ficam presas à visão do jogador, então acompanham a câmera.
        // Também calculo um polígono 2D para saber qual carta recebeu o clique do mouse.
        void cartaNaMao(int indice,boolean elevada) {
            double centroX=(indice-1)*.92, centroY=-1.72+(elevada?.20:0)-maoBaixa*2.2;
            double angulo=(1-indice)*.10;
            Render3D.V[] v=new Render3D.V[4];
            double[] xs={-.59,.59,.59,-.59}, ys={-.78,-.78,.78,.78};
            Polygon hit=new Polygon();
            for(int j=0;j<4;j++) {
                double x=centroX+xs[j]*Math.cos(angulo)-ys[j]*Math.sin(angulo);
                double y=centroY+xs[j]*Math.sin(angulo)+ys[j]*Math.cos(angulo);
                double distancia = 1.8-indice*.01-(elevada?.04:0);
                v[j]=render.camera(x*1.8/4.15,y*1.8/4.15,distancia);
                Render3D.V tela=render.p(v[j].x,v[j].y,v[j].z,0,0);
                hit.addPoint((int)tela.x,(int)tela.y);
            }
            mao[indice]=hit;
            render.quadTextura(v[0].x,v[0].y,v[0].z,v[1].x,v[1].y,v[1].z, v[2].x,v[2].y,v[2].z,v[3].x,v[3].y,v[3].z,cartas[indice]);
        }
    }
