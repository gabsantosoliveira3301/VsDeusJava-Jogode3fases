import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

/**
 * Cuida só das falas do personagem. Separei isso porque a janela não precisa
 * saber como cada letra aparece ou como o texto quebra de linha.
 */
class Fala {
        String texto = "";
        int letras;
        double espera;

        // Quando a mensagem muda, começo do zero. Se for a mesma frase, não reinicio
        // a digitação só porque algum botão foi atualizado.
        void dizer(String novo) {
            if (texto.equals(novo)) return;
            texto = novo;
            letras = 0;
            espera = .03;
        }
        boolean falando() { return letras < texto.length(); }

        // Se o jogador clicar na faixa do texto, mostro a frase inteira de uma vez.
        void completar() { letras = texto.length(); }

        // Atualizo a quantidade de letras usando o tempo que passou. Pontuação ganha
        // uma pausa um pouco maior, para o texto não parecer uma máquina digitando tudo igual.
        void atualizar(double dt) {
            espera -= dt;
            while (espera <= 0 && falando()) {
                char c = texto.charAt(letras++);
                espera += c=='.' || c=='?' || c=='!' ? .14 : c==',' ? .08 : .023;
            }
        }
        // Primeiro calculo as linhas com a frase inteira. Se eu fizesse isso enquanto
        // as letras aparecem, as palavras poderiam ficar mudando de lugar no meio da fala.
        // No fim devolvo o retângulo da faixa para a janela saber onde o clique vale.
        Rectangle desenhar(Graphics2D original, int largura, int topo, double tempo) {
            Graphics2D g = (Graphics2D)original.create();
            int tamanho = largura < 900 ? 17 : 20;
            g.setFont(new Font("Monospaced", Font.BOLD, tamanho));
            FontMetrics fm = g.getFontMetrics();
            int limite = Math.max(180, Math.min(860, largura-100));
            List<String> linhas = new ArrayList<>();
            String linha = "";
            // Vou colocando palavra por palavra até chegar perto da largura máxima.
            for (String palavra : texto.toUpperCase(Locale.ROOT).split(" ")) {
                if (!linha.isEmpty() && fm.stringWidth(linha+" "+palavra) > limite) {
                    linhas.add(linha);
                    linha = palavra;
                } else linha += (linha.isEmpty() ? "" : " ") + palavra;
            }
            if (!linha.isEmpty()) linhas.add(linha);
            int pos = 0, y = topo+tamanho;
            for (String trecho : linhas) {
                int x = (largura-fm.stringWidth(trecho))/2;
                for (char c : trecho.toCharArray()) {
                    if (pos < letras) {
                        // O tremor é bem pequeno; é só para a fala não ficar totalmente parada.
                        int tremor = falando() ? (int)Math.round(Math.sin(tempo*7+pos*1.7)*.65) : 0;
                        String letra = String.valueOf(c);
                        g.setColor(new Color(0,0,0,220));
                        for (int dx=-1;dx<=1;dx++) for (int dy=-1;dy<=1;dy++)
                            g.drawString(letra,x+dx,y+dy+tremor);
                        g.setColor(JogoCartas.cinza(231));
                        g.drawString(letra,x,y+tremor);
                    }
                    x += fm.charWidth(c);
                    pos++;
                }
                pos++; // Conta o espaço entre as linhas.
                y += tamanho+5;
            }
            g.dispose();
            return new Rectangle((largura-limite)/2, topo-4, limite, Math.max(30,y-topo));
        }
    }
