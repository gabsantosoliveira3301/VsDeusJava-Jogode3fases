import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

/**
 * Cria as imagens usadas pelo jogo em tempo de execução. Eu desenho tudo com
 * Graphics2D e BufferedImage, então não preciso carregar PNG ou JPG de fora.
 */
class Texturas {
        // Fundo comum das cartas. Depois cada tipo recebe o desenho da pedra,
        // do papel ou da tesoura por cima desta base.
        static BufferedImage base(int tipo) {
            BufferedImage im = new BufferedImage(128, 176, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = im.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(JogoCartas.cinza(13)); g.fillRect(0, 0, 128, 176);
            g.setColor(JogoCartas.cinza(215)); g.fillRect(5, 5, 118, 166);
            g.setColor(JogoCartas.cinza(174)); g.fillRect(9, 9, 110, 158);
            g.setColor(JogoCartas.cinza(32)); g.drawRect(13, 13, 102, 150);
            g.drawLine(18, 31, 110, 31); g.drawLine(18, 139, 110, 139);
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            String nome = tipo == 0 ? "PEDRA" : tipo == 1 ? "PAPEL" : tipo == 2 ? "TESOURA" : "ARQUIVO";
            g.drawString(nome, (128 - g.getFontMetrics().stringWidth(nome)) / 2, 155);
            g.dispose();
            // Coloco pequenas variações no cinza para a carta não parecer uma placa lisa.
            // A semente depende do tipo, então o desgaste de cada carta é sempre igual.
            Random desgaste = new Random(31 + tipo);
            for (int y=10;y<167;y++) for(int x=10;x<118;x++) {
                int cinza = im.getRGB(x,y)&255;
                int tom = Math.max(0, Math.min(255, cinza-desgaste.nextInt(14)));
                im.setRGB(x,y,(tom<<16)|(tom<<8)|tom);
            }
            return im;
        }
        // Faço as três figuras com polígonos e linhas. Não são imagens importadas.
        static BufferedImage[] criarCartas() {
            BufferedImage[] r = new BufferedImage[3];
            for (int i = 0; i < 3; i++) {
                r[i] = base(i); Graphics2D g = r[i].createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setStroke(new BasicStroke(4));
                g.setColor(JogoCartas.cinza(32));
                if (i == 0) {
                    Polygon pedra = new Polygon(new int[]{33, 26, 43, 74, 93, 104, 95, 75, 45},
                        new int[]{102, 80, 54, 47, 58, 83, 111, 126, 118}, 9);
                    g.fillPolygon(pedra);
                    g.setColor(JogoCartas.cinza(99));
                    g.drawLine(43, 54, 54, 82); g.drawLine(54, 82, 35, 105);
                    g.drawLine(74, 47, 71, 72); g.drawLine(71, 72, 95, 111);
                    g.setColor(Color.BLACK); g.drawLine(54, 82, 71, 72);
                } else if (i == 1) {
                    Polygon folha = new Polygon(new int[]{35, 81, 94, 94, 83, 33},
                        new int[]{47, 47, 62, 124, 130, 127}, 6);
                    g.fillPolygon(folha);
                    g.setColor(JogoCartas.cinza(95));
                    g.drawLine(81, 47, 81, 64); g.drawLine(81, 64, 94, 64);
                    for (int y = 78; y < 119; y += 11) g.drawLine(43, y, 83, y);
                } else {
                    g.drawOval(27, 99, 24, 25); g.drawOval(71, 99, 24, 25);
                    g.drawLine(42, 100, 92, 47); g.drawLine(82, 100, 36, 47);
                    g.setStroke(new BasicStroke(8));
                    g.drawLine(64, 75, 98, 41); g.drawLine(64, 75, 31, 41);
                    g.setColor(JogoCartas.cinza(32)); g.fillOval(59, 70, 10, 10);
                }
                g.dispose();
            }
            return r;
        }
        // Verso usado nas cartas que ainda não foram reveladas.
        static BufferedImage costas() {
            BufferedImage im = base(3); Graphics2D g = im.createGraphics();
            g.setColor(JogoCartas.cinza(40));
            for (int i = 0; i < 5; i++) g.drawRect(26 + i * 6, 45 + i * 6, 76 - i * 12, 80 - i * 12);
            g.drawLine(26, 45, 102, 125); g.drawLine(102, 45, 26, 125);
            g.dispose(); return im;
        }
        // Um rosto simples para o elemento que aparece na sala do quiz.
        static BufferedImage rostoJava() {
            BufferedImage im=new BufferedImage(96,96,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=im.createGraphics();
            g.setColor(JogoCartas.cinza(22));g.fillRect(0,0,96,96);
            g.setColor(JogoCartas.cinza(196));
            g.setStroke(new BasicStroke(4));
            g.drawArc(29,48,38,19,180,180);
            g.drawLine(30,57,36,70);g.drawLine(66,57,60,70);g.drawLine(36,70,60,70);
            g.drawArc(62,53,17,13,-100,210);
            g.drawArc(42,24,12,26,95,140);g.drawArc(52,18,11,30,95,140);
            g.setFont(new Font("Monospaced",Font.BOLD,12));g.drawString("JAVA",32,87);
            g.dispose();return im;
        }
        // Quatro quadros para alternar enquanto o personagem está falando.
        // A mudança é pequena, mas já dá a impressão de uma animação na tela dele.
        static BufferedImage[] facesJava() {
            BufferedImage[] faces=new BufferedImage[4];
            for(int quadro=0;quadro<faces.length;quadro++) {
                BufferedImage im=new BufferedImage(96,96,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=im.createGraphics();
                g.setColor(Color.BLACK);g.fillRect(0,0,96,96);
                g.setColor(JogoCartas.cinza(21));
                for(int y=2;y<96;y+=4) g.drawLine(2,y,93,y);
                g.setFont(new Font("Monospaced",Font.BOLD,7));
                g.setColor(JogoCartas.cinza(90));g.drawString("JAVA.EXE",5,9);
                g.drawLine(5,13,90,13);
                int movimento=quadro==0 ? 0 : quadro-2;
                g.setColor(new Color(239,112,30));
                g.setStroke(new BasicStroke(4,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER));
                g.drawArc(33+movimento,44,30,13,190,160);
                g.drawLine(34+movimento,52,39,62);
                g.drawLine(62+movimento,52,57,62);
                g.drawLine(39,62,57,62);
                g.drawArc(60,48,14,12,-110,200);
                g.setStroke(new BasicStroke(3,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER));
                g.drawPolyline(new int[]{42+movimento,47+movimento,43+movimento,49+movimento},
                               new int[]{39,34,28,22},4);
                g.drawPolyline(new int[]{53-movimento,57-movimento,52-movimento,58-movimento},
                               new int[]{40,34,27,20},4);
                g.setFont(new Font("Monospaced",Font.BOLD,13));
                g.drawString("JAVA",31,79);
                g.setFont(new Font("Monospaced",Font.BOLD,8));
                g.drawString(quadro==0 ? "> _" : "> ...",5,90);
                g.dispose();faces[quadro]=im;
            }
            return faces;
        }
        // Livro que fica na mesa da segunda sala.
        static BufferedImage livro() {
            BufferedImage im=base(3);Graphics2D g=im.createGraphics();
            g.setColor(JogoCartas.cinza(20));g.setFont(new Font("Monospaced",Font.BOLD,14));
            g.drawString("JAVA",40,54);
            g.drawString("?",58,105);g.dispose();return im;
        }
        // Textura procedural bem simples de madeira, feita pixel por pixel.
        static BufferedImage madeira() {
            BufferedImage im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            Random r = new Random(9);
            for (int y=0; y<64; y++) for (int x=0; x<64; x++) {
                int shade=37+(int)(Math.sin(y*.7+Math.sin(x*.13))*5)+r.nextInt(7);
                if (y==32 || y==0) shade=22;
                im.setRGB(x,y,(shade<<16)|(shade<<8)|shade);
            }
            return im;
        }
    }
