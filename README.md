# Vs Deus Java  


Um protótipo simples 3D da atividade de um jogo em 3 Fases.

Esta versão foi feita **diretamente a partir do JogoCartas.java original com 1.124 linhas** (versão bruta, deu trabalho de simplificar o código.)

## Onde eu fiz e onde tercerizei o trabalho.

| Arquivo | O que faz | Principais alterações |
| --- | --- | --- |
| `JogoCartas.java` | Regras das 3 fases, pontuação, respostas, JFrame e botões | Editar `JAVA`, `CURIOSIDADES`, `jogar()`, `responder()` e `atualizarTela()` | Aqui foi feito 100% por mim, do começo ao fim.
| `Mesa3D.java` | Posiciona mesas, cartas, corredor, velas e o oponente | Métodos `sala()`, `mesa()`, `adversario()`, `vela()` | aqui foi o único modelo 3D que me aventurei a tentar importar eu mesmo
| `Texturas.java` | Desenha as imagens das cartas em memória | `base()` e `criarCartas()` | texturas simples geradas proceduralmente fiz por tentativa e erro pra ser sincero
| `Animacao.java` | Movimento da câmera, cartas, balança e porta | Constantes `DURACAO`, `CAMINHADA` e `CAMINHO` | keyframes CRIADAS POR MIM portadas utilizando IA para o render3D por software.
| `Fala.java` | Texto aparecendo aos poucos | `atualizar()` e `desenhar()` |
| `Render3D.java` | Motor gráfico próprio usando BufferedImage | `projetar()`, `caixa()`, `triangulo()` | Aqui eu Fiz ele porém com ressalvas.

**Por que ainda existem várias linhas?** A maior parte implementa o desenho 3D em software sem bibliotecas externas. Tentei diminuir pra facilitar a avaliação da atividade e até pra eu explicar melhor na hora de apresentar, porém basicamente essa parte foi feita importando dados do blender pra instruções de formação da geometria e animações keyframes. (essa parte utilizei um algoritmo criado por IA pra importar, os conteúdos e animações foram feitos por mim)

## Abrir no Eclipse

1. Descompacte o ZIP.
2. No Eclipse: **File > Import > General > Existing Projects into Workspace**.
3. Selecione a pasta `JogoCartas-fiel` e conclua.
4. Abra `src/JogoCartas.java` e execute **Run As > Java Application**.

Requer um JDK moderno (Java 11 ou superior). Sem dependências externas.

## Compilar manualmente

```bash
mkdir -p bin
javac -encoding UTF-8 -d bin src/*.java
java -cp bin JogoCartas
```

## Resumo rápido do que tem no código fonte.

1. `JogoCartas` é um `JFrame`: ele mostra a janela e os botões.
2. `iniciar()` zera pontos e começa o duelo.
3. `jogar()` resolve pedra, papel e tesoura com números aleatórios.
4. `responder()` verifica o índice da resposta, soma os pontos e `avancarQuiz()` muda a pergunta ou fase.
5. `Mesa3D` organiza o cenário com caixas e polígonos; `Texturas` produz as cartas sem arquivos de imagem.
6. `Render3D` é a parte mais técnica: converte pontos tridimensionais em pixels com a conta da perspectiva `x * foco / z` e usa profundidade para desenhar a frente dos objetos sobre o fundo. (essa parte foi feita pra substituir assets que eu criei no blender, funciona porém utilizei um algoritmo criado por IA pra importar os assets para instruções do renderizador por software.)

## Refatoração do código que fiz. 


- Captura visual automatizada do original e da versão refatorada na mesma resolução; cenário e cartas permaneceram praticamente idênticos.
- Teste automatizado das três fases até o resultado final.
- A trajetória da câmera na caminhada teve a interpolação Hermite substituída por interpolação suave mais simples; o percurso conserva seus pontos, embora a velocidade da transição possa variar um pouco. (implementação bem simples, olhei em algumas wikis)
- A balança usa uma atualização de mola simplificada. (removi a necessidade de algumas estruturas de repetição, ele verifica o estado da balança apenas se a condição da animação da balança estiver false.)

`capturas/original.png` e `capturas/refatorado.png` servem como preview pro projeto.
