import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

/**
 * É a parte mais matemática do projeto. Eu não usei uma engine 3D pronta:
 * este código transforma pontos, monta triângulos e decide quais pixels aparecem.
 * Não é um renderizador geral, foi feito para a cena deste jogo.
 */
class Render3D {
        double deslocamentoZ, deslocamentoY, luminosidade=1;
        final double[] chamas={1,1};
        double cameraX, cameraY=3, cameraZ=-6;
        double direitaX=1, direitaZ, cimaX, cimaY=.948683298, cimaZ=.316227766;
        double frenteX, frenteY=-.316227766, frenteZ=.948683298, foco=310;
        // Recebo a posição da câmera e o ponto para onde ela olha. A partir disso
        // monto três direções (frente, direita e cima) para orientar o cenário.
        void configurarCamera(double[] vista) {
            cameraX=vista[0]; cameraY=vista[1]; cameraZ=vista[2];
            double x=vista[3]-cameraX, y=vista[4]-cameraY, z=vista[5]-cameraZ;
            double comprimento=Math.sqrt(x*x+y*y+z*z);
            frenteX=x/comprimento; frenteY=y/comprimento; frenteZ=z/comprimento;
            double horizontal=Math.sqrt(frenteX*frenteX+frenteZ*frenteZ);
            direitaX=frenteZ/horizontal; direitaZ=-frenteX/horizontal;
            cimaX=direitaZ*frenteY;
            cimaY=direitaX*frenteZ-direitaZ*frenteX;
            cimaZ=-direitaX*frenteY;
            foco=h*.5/Math.tan(Math.toRadians(vista[6]*.5));
        }
        final int w, h; final BufferedImage imagem;
        // Pixels é a própria imagem como uma lista de cores. O outro array guarda
        // a distância do objeto desenhado em cada posição, para esconder o que fica atrás.
        final int[] pixels; final float[] profundidade;
        Render3D(int w, int h) {
            this.w = w; this.h = h;
            imagem = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            pixels = ((DataBufferInt)imagem.getRaster().getDataBuffer()).getData();
            profundidade = new float[w * h];
        }
        // Limpo tanto a cor quanto a profundidade antes de começar um quadro novo.
        void limpar() {
            Arrays.fill(pixels, 0x080808);
            Arrays.fill(profundidade, Float.POSITIVE_INFINITY);
        }
        // Um ponto também carrega u/v, que são as coordenadas usadas para buscar
        // um pedaço da textura. Assim não preciso guardar outra estrutura separada.
        static class V {
            double x, y, z, u, v;
            V(double x, double y, double z, double u, double v) {
                this.x = x; this.y = y; this.z = z; this.u = u; this.v = v;
            }
        }
        // Não deixo nada passar para trás deste limite, porque dividir por uma
        // distância zero ou negativa estragaria a projeção da perspectiva.
        static final double PERTO=.12;

        // Transformo a posição do mundo para o referencial da câmera.
        V vista(double x,double y,double z,double u,double v) {
            double dx=x-cameraX,dy=y+deslocamentoY-cameraY,dz=z+deslocamentoZ-cameraZ;
            return new V(dx*direitaX+dz*direitaZ, dx*cimaX+dy*cimaY+dz*cimaZ,
                         dx*frenteX+dy*frenteY+dz*frenteZ,u,v);
        }
        // Aqui acontece a perspectiva: quanto maior a distância z, menor o objeto na tela.
        V projetar(V a) {
            return new V(w/2.0+a.x*foco/a.z,h*.43-a.y*foco/a.z,a.z,a.u,a.v);
        }
        V p(double x,double y,double z,double u,double v) { return projetar(vista(x,y,z,u,v)); }
        // Quando uma aresta cruza o limite próximo da câmera, calculo o ponto de corte.
        V intersecao(V a,V b) {
            double t=(PERTO-a.z)/(b.z-a.z);
            return new V(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,PERTO,
                         a.u+(b.u-a.u)*t,a.v+(b.v-a.v)*t);
        }
        // Um triângulo pode ficar inteiro na frente da câmera, inteiro atrás ou cruzar
        // o limite. Neste último caso eu corto a parte que ficaria perto demais e desenho o resto.
        void recortar(V a,V b,V c,BufferedImage textura,int luz) {
            if(a.z>=PERTO && b.z>=PERTO && c.z>=PERTO) {
                triangulo(projetar(a),projetar(b),projetar(c),textura,luz); return;
            }
            if(a.z<PERTO && b.z<PERTO && c.z<PERTO) return;
            V[] entrada={a,b,c},saida=new V[4]; int quantidade=0;
            V anterior=c;
            for(V atual:entrada) {
                boolean dentro=atual.z>=PERTO, antes=anterior.z>=PERTO;
                if(dentro!=antes) saida[quantidade++]=intersecao(anterior,atual);
                if(dentro) saida[quantidade++]=atual;
                anterior=atual;
            }
            for(int i=1;i<quantidade-1;i++)
                triangulo(projetar(saida[0]),projetar(saida[i]),projetar(saida[i+1]),textura,luz);
        }
        // Faço o caminho inverso da câmera para colocar as cartas na frente do jogador.
        // Por isso elas continuam no mesmo lugar da tela mesmo quando a câmera passeia.
        V camera(double x,double y,double z) {
            x*=310/foco; y*=310/foco;
            return new V(cameraX+direitaX*x+cimaX*y+frenteX*z,
                         cameraY+cimaY*y+frenteY*z,
                         cameraZ+direitaZ*x+cimaZ*y+frenteZ*z,0,0);
        }

        // O renderizador trabalha com triângulos. Então cada quadrilátero vira dois,
        // e eu reaproveito o mesmo código para paredes, chão e móveis.
        void quad(double ax,double ay,double az,double bx,double by,double bz,
                  double cx,double cy,double cz,double dx,double dy,double dz,int luz) {
            V a=vista(ax,ay,az,0,0), b=vista(bx,by,bz,1,0), c=vista(cx,cy,cz,1,1), d=vista(dx,dy,dz,0,1);
            int iluminado=(int)(luz*luzLocal((ax+bx+cx+dx)/4,(ay+by+cy+dy)/4,(az+bz+cz+dz)/4));
            recortar(a,b,c,null,iluminado); recortar(a,c,d,null,iluminado);
        }
        // É a mesma ideia do quad normal, mas agora cada vértice também recebe u/v
        // para saber qual pedaço da imagem vai ficar em cada parte da superfície.
        void quadTextura(double ax,double ay,double az,double bx,double by,double bz,
                         double cx,double cy,double cz,double dx,double dy,double dz,BufferedImage textura) {
            V a=vista(ax,ay,az,0,1), b=vista(bx,by,bz,1,1), c=vista(cx,cy,cz,1,0), d=vista(dx,dy,dz,0,0);
            int iluminado=(int)(255*luzLocal((ax+bx+cx+dx)/4,(ay+by+cy+dy)/4,(az+bz+cz+dz)/4));
            recortar(a,b,c,textura,iluminado); recortar(a,c,d,textura,iluminado);
        }
        // Misturo uma luz ambiente com a luz das velas. É uma aproximação simples:
        // quanto mais longe da vela, menor a contribuição dela para o brilho.
        double luzLocal(double x,double y,double z) {
            double luz=.34;
            double[] lx={-3.2,3.2,-4.5,4.5}, lz={4.4,4.4,19,19};
            for(int i=0;i<4;i++) {
                double dx=x-lx[i],dy=y-1.5,dz=z-lz[i];
                double forca=i<2 ? chamas[i] : 1;
                luz+=.90*forca/(1+.12*(dx*dx+dy*dy+dz*dz));
            }
            return Math.min(1.14,luz);
        }
        // Para mim é mais fácil montar o cenário usando caixas. Cada uma tem seis
        // faces, e o z-buffer resolve quais precisam ficar na frente das outras.
        void caixa(double x,double y,double z,double rx,double ry,double rz,int cor) {
            quad(x-rx,y-ry,z-rz,x+rx,y-ry,z-rz,x+rx,y+ry,z-rz,x-rx,y+ry,z-rz,cor);
            quad(x+rx,y-ry,z+rz,x-rx,y-ry,z+rz,x-rx,y+ry,z+rz,x+rx,y+ry,z+rz,cor-11);
            quad(x-rx,y+ry,z-rz,x+rx,y+ry,z-rz,x+rx,y+ry,z+rz,x-rx,y+ry,z+rz,cor+20);
            quad(x-rx,y-ry,z+rz,x+rx,y-ry,z+rz,x+rx,y-ry,z-rz,x-rx,y-ry,z-rz,cor-17);
            quad(x-rx,y-ry,z+rz,x-rx,y-ry,z-rz,x-rx,y+ry,z-rz,x-rx,y+ry,z+rz,cor-16);
            quad(x+rx,y-ry,z-rz,x+rx,y-ry,z+rz,x+rx,y+ry,z+rz,x+rx,y+ry,z-rz,cor-7);
        }
        // Esta conta ajuda a descobrir de que lado da aresta está um ponto.
        // Uso isso para testar se um pixel está dentro do triângulo.
        double borda(V a,V b,double x,double y) { return (x-a.x)*(b.y-a.y)-(y-a.y)*(b.x-a.x); }

        // Aqui acontece o desenho de verdade: percorro a área do triângulo e pinto
        // só os pixels que caem dentro dele. Se tiver textura, busco a cor correspondente.
        void triangulo(V a,V b,V c,BufferedImage textura,int brilho) {
            if (a.z <= .1 || b.z <= .1 || c.z <= .1) return;
            double area = borda(a,b,c.x,c.y);
            if (Math.abs(area) < .01) return;
            int x0=Math.max(0,(int)Math.floor(Math.min(a.x,Math.min(b.x,c.x))));
            int x1=Math.min(w-1,(int)Math.ceil(Math.max(a.x,Math.max(b.x,c.x))));
            int y0=Math.max(0,(int)Math.floor(Math.min(a.y,Math.min(b.y,c.y))));
            int y1=Math.min(h-1,(int)Math.ceil(Math.max(a.y,Math.max(b.y,c.y))));
            int[] tex = textura == null ? null : ((DataBufferInt)textura.getRaster().getDataBuffer()).getData();
            for (int y=y0;y<=y1;y++) for(int x=x0;x<=x1;x++) {
                double q0=borda(b,c,x+.5,y+.5)/area;
                double q1=borda(c,a,x+.5,y+.5)/area;
                double q2=1-q0-q1;
                if(q0<0||q1<0||q2<0) continue;
                // O z-buffer compara a distância do que está sendo desenhado com a
                // distância que já estava neste pixel. O menor valor fica na frente.
                // Uso 1/z na interpolação para a textura não ficar torta na perspectiva.
                double iz=q0/a.z+q1/b.z+q2/c.z;
                if(iz<=0) continue;
                float z=(float)(1/iz); int pos=y*w+x;
                if(z>=profundidade[pos]) continue;
                profundidade[pos]=z;
                int cor=0xFFFFFF;
                if(tex!=null) {
                    // Interpolo u e v levando a distância em conta; assim a textura
                    // acompanha a perspectiva em vez de parecer colada de frente.
                    double u=(q0*a.u/a.z+q1*b.u/b.z+q2*c.u/c.z)/iz;
                    double v=(q0*a.v/a.z+q1*b.v/b.z+q2*c.v/c.z)/iz;
                    int tx=Math.max(0,Math.min(textura.getWidth()-1,(int)(u*(textura.getWidth()-1))));
                    int ty=Math.max(0,Math.min(textura.getHeight()-1,(int)(v*(textura.getHeight()-1))));
                    cor=tex[ty*textura.getWidth()+tx];
                }
                double intensidade=luminosidade*brilho/255.0;
                pixels[pos]=(canal(cor,16,intensidade)<<16) |
                            (canal(cor,8,intensidade)<<8) | canal(cor,0,intensidade);
            }
        }
        // Aplico a intensidade em cada canal RGB sem deixar a cor passar de 255.
        static int canal(int cor,int bits,double luz) {
            return Math.min(255,(int)(((cor>>bits)&255)*luz));
        }
    }
