// CannonView.java
// Exibe e controla o aplicativo Cannon Game
package com.deitel.cannongame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CannonView extends SurfaceView
        implements SurfaceHolder.Callback
{
    private static final String TAG = "CannonView";     // para registrar erros

    private CannonThread cannonThread;      // controla o loop do jogo
    private Activity activity;      // para exibir a caixa de diálogo Game Over na thread da interface gráfica do usuário
    private boolean dialogIsDisplayed = false;

    // constantes para interação do jogo
    public static final int TARGET_PIECES = 7;    // seções no alvo
    public static final int MISS_PENALTY = 2;    // segundos subtraídos em caso de erro
    public static final int HIT_REWARD = 3;    // segundos adicionados em caso de acerto

    // variáveis para o loop do jogo e controle de estatísticas
    private boolean gameOver;           // o jogo terminou?
    private double timeLeft;            // tempo restante em segundos
    private int shotsFired;             // tiros disparados pelo usuário
    private double totalElapsedTime;    // segundos decorridos

    // variáveis para a barreira e para o alvo
    private Line blocker;               // pontos inicial e final da barreira
    private int blockerDistance;        // distância da barreira a partir da esquerda
    private int blockerBeginning;       // distância do topo da barreira até a parte superior
    private int blockerEnd;             // distância da parte inferior da barreira até o topo
    private int initialBlockerVelocity; // multiplicador de velocidade inicial da barreira
    private float blockerVelocity;      // multiplicador de velocidade da barreira durante o jogo

    private Line target;                // pontos inicial e final do alvo
    private int targetDistance;         // distância do alvo a partir da esquerda
    private int targetBeginning;        // distância do alvo a partir do topo
    private double pieceLength;         // comprimento de uma parte do alvo
    private int targetEnd;              // distância da parte inferior do alvo a partir do topo
    private int initialTargetVelocity;  // multiplicador de velocidade inicial do alvo
    private float targetVelocity;       // multiplicador de velocidade do alvo

    private int lineWidth;              // largura do alvo e da barreira
    private boolean[] hitStates;        // cada parte do alvo foi atingida?
    private int targetPiecesHit;        // número de partes do alvo atingidas (até 7)

    // variáveis para o canhão e para a bala
    private Point cannonball;           // canto superior esquerdo da imagem da bala
    private int cannonballVelocityX;    // velocidade x da bala
    private int cannonballVelocityY;    // velocidade y da bala
    private boolean cannonballOnScreen; // se a bala está na tela ou não
    private int cannonballRadius;       // raio da bala
    private int cannonballSpeed;        // velocidade da bala
    private int cannonBaseRadius;       // raio da base do canhão
    private int cannonLength;   // comprimento do cano do canhão
    private Point barrelEnd;    // o ponto extremo do cano do canhão
    private int screenWidth;
    private int screenHeight;

    // constantes e variáveis para gerenciar sons
    private static final int TARGET_SOUND_ID = 0;
    private static final int CANNON_SOUND_ID = 1;
    private static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // reproduz os efeitos sonoros
    private SparseIntArray soundMap; // mapeia identificadores em SoundPool

    // variáveis Paint utilizadas ao desenhar cada item na tela
    private Paint textPaint;          // objeto Paint usado para desenhar texto
    private Paint cannonballPaint;    // objeto Paint usado para desenhar a bala de canhão
    private Paint cannonPaint;        // objeto Paint usado para desenhar o canhão
    private Paint blockerPaint;       // objeto Paint usado para desenhar a barreira
    private Paint targetPaint;        // objeto Paint usado para desenhar o alvo
    private Paint backgroundPaint;    // objeto Paint usado para limpar a área de desenho

    // construtor public
    public CannonView(Context context, AttributeSet attrs)
    {
        super(context, attrs);          // chama o construtor da superclasse
        activity = (Activity) context;  // armazena referência para MainActivity

        // registra o receptor SurfaceHolder.Callback
        getHolder().addCallback(this);

        // inicializa Lines e Point representando itens do jogo
        blocker = new Line();        // cria a barreira como um objeto Line
        target = new Line();        // cria o alvo como um objeto Line
        cannonball = new Point();   // cria a bala de canhão como um objeto Point

        // inicializa hitStates como um array booleano
        hitStates = new boolean[TARGET_PIECES];

        // inicializa SoundPool para reproduzir os três efeitos sonoros do aplicativo
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        // cria objeto Map de sons e carrega os sons previamente
        soundMap = new SparseIntArray(3);       // cria novo objeto SparseIntArray
        soundMap.put(TARGET_SOUND_ID,
                soundPool.load(context, R.raw.target_hit,1));
        soundMap.put(CANNON_SOUND_ID,
                soundPool.load(context, R.raw.cannon_fire,1));
        soundMap.put(BLOCKER_SOUND_ID,
                soundPool.load(context, R.raw.blocker_hit,1));

        // constrói objetos Paint para desenhar o texto, a bala, o canhão,
        // a barreira e o alvo; eles são configurados no método onSizeChanged
        textPaint = new Paint();
        cannonPaint = new Paint();
        cannonballPaint = new Paint();
        blockerPaint = new Paint();
        targetPaint = new Paint();
        backgroundPaint = new Paint();
    }   // fim do construtor de CannonView

    // chamado por surfaceChanged quando o tamanho do componente SurfaceView
    // muda, como quando ele é adicionado à hierarquia de Views
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

    screenWidth = w;    // armazena a largura de CannonView
    screenHeight = h;    // armazena a altura de CannonView
    cannonBaseRadius = h / 18;    // o raio da base do canhão tem 1/18 da altura da tela
    cannonLength = w / 8;    // o comprimento do canhão tem 1/8 da largura da tela

    cannonballRadius = w / 36;    // o raio da bala tem 1/36 da largura da tela
    cannonballSpeed = w * 3 / 2;    // multiplicador de velocidade da bala

    lineWidth = w / 24;    // o alvo e a barreira têm 1/24 da largura da tela

    // configura variáveis de instância relacionadas à barreira
    blockerDistance = w * 5 / 8;    // a barreira tem 5/8 da largura da tela a partir da esquerda
    blockerBeginning = h / 8;    // a distância a partir do topo é de 1/8 da altura da tela
    blockerEnd = h * 3 / 8;    // a distância a partir do topo é de 3/8 da altura da tela
    initialBlockerVelocity = h / 2;    // multiplicador de velocidade inicial da barreira
    blocker.start = new Point(blockerDistance, blockerBeginning);
    blocker.end = new Point(blockerDistance, blockerEnd);

    // configura variáveis de instância relacionadas ao alvo
    targetDistance = w * 7 / 8;    // o alvo tem 7/8 da largura da tela a partir da esquerda
    targetBeginning = h / 8;    // a distância a partir do topo é de 1/8 da altura da tela
    targetEnd = h * 7 / 8;    // a distância a partir do topo é de 7/8 da altura da tela
    pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
    initialTargetVelocity = -h / 4;    // multiplicador de velocidade inicial do alvo
    target.start = new Point(targetDistance, targetBeginning);
    target.end = new Point(targetDistance, targetEnd);

    // o ponto extremo do cano do canhão aponta horizontalmente no início
    barrelEnd = new Point(cannonLength, h / 2);

    // configura objetos Paint para desenhar os elementos do jogo
    textPaint.setTextSize(w / 20);  // o tamanho do texto tem 1/20 da largura da tela
    textPaint.setAntiAlias(true);   // suaviza o texto
    cannonPaint.setStrokeWidth(lineWidth * 1.5f); // configura a espessura da linha
    blockerPaint.setStrokeWidth(lineWidth); // configura a espessura da linha
    targetPaint.setStrokeWidth(lineWidth); // configura a espessura da linha
    backgroundPaint.setColor(Color.WHITE); // configura a cor de fundo

    newGame();    // prepara e inicia um novo jogo
    }// fim do método onSizeChanged

    // reinicia todos os elementos de tela e inicia um novo jogo
    public void newGame()
    {
        // configura cada elemento de hitStates como false -- restaura partes do alvo
        for (int i = 0; i < TARGET_PIECES; i++)
            hitStates[i] = false;

        targetPiecesHit = 0;        // nenhuma parte do alvo foi atingida
        blockerVelocity = initialBlockerVelocity;        // configura a velocidade inicial
        targetVelocity = initialTargetVelocity;        // configura a velocidade inicial
        timeLeft = 10; // inicia a contagem regressiva em 10 segundos
        cannonballOnScreen = false; // a bala de canhão não está na tela
        shotsFired = 0; // configura o número inicial de tiros disparados
        totalElapsedTime = 0.0; // configura o tempo decorrido como zero

        // configura os objetos Point inicial e final da barreira e do alvo
        blocker.start.set(blockerDistance, blockerBeginning);
        blocker.end.set(blockerDistance, blockerEnd);
        target.start.set(targetDistance, targetBeginning);
        target.end.set(targetDistance, targetEnd);

        if (gameOver) // iniciando um novo jogo depois que o último terminou
            {
                gameOver = false; // o jogo não terminou
                cannonThread = new CannonThread(getHolder()); // cria thread
                cannonThread.start(); // inicia a thread do loop do jogo
                } // fim do if
        } // fim do método newGame

        // chamado repetidamente por CannonThread para atualizar os elementos do jogo
        private void updatePositions(double elapsedTimeMS)
        {
            double interval = elapsedTimeMS / 1000.0; // converte em segundos

                if (cannonballOnScreen) // se um tiro foi disparado no momento
                    {
                        // atualiza a posição da bala de canhão
                        cannonball.x += interval * cannonballVelocityX;
                        cannonball.y += interval * cannonballVelocityY;

                        // verifica se houve colisão com a barreira
                        if (cannonball.x + cannonballRadius > blockerDistance &&
                                cannonball.x - cannonballRadius < blockerDistance &&
                                cannonball.y + cannonballRadius > blocker.start.y &&
                                cannonball.y - cannonballRadius < blocker.end.y)
                        {
                            cannonballVelocityX *= -1; // direção inversa da bala de canhão
                            timeLeft -= MISS_PENALTY; // penaliza o usuário

                            // reproduz o som da barreira
                            soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);
                            }
                            // verifica se houve colisões com as paredes esquerda e direita
                        else if (cannonball.x + cannonballRadius > screenWidth ||
                                cannonball.x - cannonballRadius < 0)
                        { soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);
                        cannonballOnScreen = false; // remove a bala de canhão da tela
                         }
                         // verifica se houve colisões com as paredes superior e inferior
                        else if (cannonball.y + cannonballRadius > screenHeight ||
                                cannonball.y - cannonballRadius < 0)
                        {
                            cannonballOnScreen = false; // remove a bala de canhão da tela
                            }
                            // verifica se houve colisão da bala com o alvo
                        else if (cannonball.x + cannonballRadius > targetDistance &&
                                cannonball.x - cannonballRadius < targetDistance &&
                                cannonball.y + cannonballRadius > target.start.y &&
                                cannonball.y - cannonballRadius < target.end.y)
                        {
                            // determina o número da seção do alvo (0 é a parte superior)
                            int section =
                                    (int) ((cannonball.y - target.start.y) / pieceLength);

                            // verifica se a parte ainda não foi atingida
                            if ((section >= 0 && section < TARGET_PIECES) &&
                                !hitStates[section])
                            {
                                hitStates[section] = true;  // a seção foi atingida
                                cannonballOnScreen = false; // remove a bala de canhão
                                timeLeft += HIT_REWARD; // acrescenta recompensa ao tempo restante

                                // reproduz o som de alvo atingido
                                soundPool.play(soundMap.get(TARGET_SOUND_ID), 1,
                                        1, 1, 0, 1f);

                                // se todas as partes foram atingidas
                                if (++targetPiecesHit == TARGET_PIECES)
                                {
                                    cannonThread.setRunning(false);     // termina a thread
                                    showGameOverDialog(R.string.win);   // mostra caixa de diálogo de vitória
                                    gameOver = true;
                                }
                            }
                        }
                    }

            // atualiza a posição da barreira
            double blockerUpdate = interval * blockerVelocity;
                blocker.start.y += blockerUpdate;
                blocker.end.y += blockerUpdate;

            // atualiza a posição do alvo
            double targetUpdate = interval * targetVelocity;
            target.start.y += targetUpdate;
            target.end.y += targetUpdate;

            // se a barreira atingiu a parte superior ou inferior, inverte a direção
            if (blocker.start.y < 0 || blocker.end.y > screenHeight)
                blockerVelocity *= -1;

            // se o alvo atingiu a parte superior ou inferior, inverte a direção
            if (target.start.y < 0 || target.end.y > screenHeight)
                targetVelocity *= -1;

            timeLeft -= interval; // subtrai do tempo restante

            // se o cronômetro foi zerado
            if (timeLeft <= 0.0)
            {
                timeLeft = 0.0;
                gameOver = true; // o jogo terminou
                cannonThread.setRunning(false); // termina a thread
                showGameOverDialog(R.string.lose); // mostra caixa de diálogo de derrota
                }
        } // fim do método updatePositions

        // dispara uma bala de canhão
        public void fireCannonball(MotionEvent event)
        {
            if (cannonballOnScreen)     // se uma bala já está na tela
                return; // nada faz

            double angle = alignCannon(event);  // obtém o ângulo do cano do canhão

            // move a bala para dentro do canhão
            cannonball.x = cannonballRadius;    // alinha a coordenada x com o canhão
            cannonball.y = screenHeight / 2;    // centraliza a bala verticalmente

            // obtém o componente x da velocidade total
                cannonballVelocityX = (int) (cannonballSpeed * Math.sin(angle));

            // obtém o componente y da velocidade total
            cannonballVelocityY = (int) (-cannonballSpeed * Math.cos(angle));
            cannonballOnScreen = true;  // a bala de canhão está na tela
            ++shotsFired; // incrementa shotsFired

            // reproduz o som de canhão disparado
            soundPool.play(soundMap.get(CANNON_SOUND_ID), 1, 1, 1, 0, 1f);
            } // fim do método fireCannonball

        // alinha o canhão em resposta a um toque do usuário
            public double alignCannon(MotionEvent event)
            {
                // obtém o local do toque nessa view de exibição
                Point touchPoint = new Point((int) event.getX(), (int) event.getY());

                // calcula a distância do toque a partir do centro da tela
                // no eixo y
                double centerMinusY = (screenHeight / 2 - touchPoint.y);

                double angle = 0; // inicializa o ângulo com 0

                // calcula o ângulo do cano em relação à horizontal
                if (centerMinusY != 0)  // evita divisão por 0
                    angle = Math.atan((double) touchPoint.x / centerMinusY);

                // se o toque foi dado na metade inferior da tela
                if (touchPoint.y > screenHeight / 2)
                    angle += Math.PI; // ajusta o ângulo

                // calcula o ponto extremo do cano do canhão
                barrelEnd.x = (int) (cannonLength * Math.sin(angle));
                barrelEnd.y =
                        (int) (-cannonLength * Math.cos(angle) + screenHeight / 2);

                return angle; // retorna o ângulo calculado
                } // fim do método alignCannon

            // desenha o jogo no objeto Canvas dado
                public void drawGameElements(Canvas canvas)
                {
                    // limpa o plano de fundo
                    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
                        backgroundPaint);

                    // exibe o tempo restante
                    canvas.drawText(getResources().getString(
                                R.string.time_remaining_format, timeLeft), 30, 50, textPaint);

                    // se uma bala de canhão está na tela, a desenha
                    if (cannonballOnScreen)
                    canvas.drawCircle(cannonball.x, cannonball.y, cannonballRadius,
                            cannonballPaint);

                    // desenha o cano do canhão
                    canvas.drawLine(0, screenHeight / 2, barrelEnd.x, barrelEnd.y,
                            cannonPaint);

                    // desenha a base do canhão
                    canvas.drawCircle(0, (int) screenHeight / 2,
                            (int) cannonBaseRadius, cannonPaint);

                    // desenha a barreira
                    canvas.drawLine(blocker.start.x, blocker.start.y, blocker.end.x,
                            blocker.end.y, blockerPaint);

                    Point currentPoint = new Point(); // início da seção do alvo atual

                    // inicializa currentPoint com o ponto inicial do alvo
                    currentPoint.x = target.start.x;
                    currentPoint.y = target.start.y;

                    // desenha o alvo
                    for (int i = 0; i < TARGET_PIECES; i++)
                    {
                        // se essa parte do alvo não foi atingida, a desenha
                        if (!hitStates[i])
                        {
                            // alterna o colorido das partes
                            if (i % 2 != 0)
                                targetPaint.setColor(Color.BLUE);
                            else
                            targetPaint.setColor(Color.YELLOW);

                           canvas.drawLine(currentPoint.x, currentPoint.y, target.end.x,
                            (int)   (currentPoint.y + pieceLength), targetPaint);
                        }

                        // move currentPoint para o início da próxima parte
                        currentPoint.y += pieceLength;
                }
            } // fim do método drawGameElements

            // exibe um componente AlertDialog quando o jogo termina
            private void showGameOverDialog(final int messageId)
            {
                // DialogFragment para exibir estatísticas do jogo e começar um novo teste
                final DialogFragment gameResult =
                        new DialogFragment()
                        {
                                // cria um componente AlertDialog e o retorna
                                @Override
                public Dialog onCreateDialog(Bundle bundle)
                                {
                                    // cria caixa de diálogo exibindo recurso String pelo messageId
                                    AlertDialog.Builder builder =
                                            new AlertDialog.Builder(getActivity());
                                    builder.setTitle(getResources().getString(messageId));

                                    // exibe o número de tiros disparados e o tempo total decorrido
                                    builder.setMessage(getResources().getString(
                                            R.string.results_format, shotsFired, totalElapsedTime));
                                    builder.setPositiveButton(R.string.reset_game,
                                            new DialogInterface.OnClickListener()
                                            {
                                                // chamado quando o componente Button “Reset Game” é pressionado
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    dialogIsDisplayed = false;
                                                    newGame(); // prepara e inicia um novo jogo
                                                    }
                                            }   // fim da classe interna anônima
                                            ); // fim da chamada a setPositiveButton

                                    return builder.create(); // retorna o componente AlertDialog
                                    } // fim do método onCreateDialog
                            }; // fim da classe interna anônima DialogFragment

                // em uma thread de interface gráfica do usuário, usa FragmentManager para exibir o componente DialogFragment
                activity.runOnUiThread (
                        new Runnable() {
                            public void run()
                            {
                                dialogIsDisplayed = true;
                                gameResult.setCancelable(false); // caixa de diálogo modal
                                gameResult.show(activity.getFragmentManager(), "results" );
                            }
                        }   // fim de Runnable
                );  // fim da chamada a runOnUiThread
                } // fim do método showGameOverDialog

        // interrompe o jogo; chamado pelo método onPause de CannonGameFragment
            public void stopGame()
            {
                if (cannonThread != null)
                    cannonThread.setRunning(false); // diz à thread para terminar
                }

                // libera recursos; chamado pelo método onDestroy de CannonGame
                public void releaseResources()
                {
                    soundPool.release(); // libera todos os recursos usados por SoundPool
                    soundPool = null;
                }

        // chamado quando o tamanho da superfície muda
            @Override
                public void (SurfaceHolder holder, int format,
                             int width, int height)
            {
            }

            // chamado quando a superfície é criada
            @Override
            public void (SurfaceHolder holder)
            {
                if (!dialogIsDisplayed)
                {
                    surfaceChanged surfaceCreated cannonThread = new CannonThread(holder); // cria a thread
                    cannonThread.setRunning(true); // começa a executar o jogo
                    cannonThread.start(); // inicia a thread do loop do jogo
                }
            }

    // chamado quando a superfície é destruída
    @Override
    public void surfaceDestroyed (SurfaceHolder holder)
    {
        // garante que essa thread termine corretamente
        boolean retry = true;
        cannonThread.setRunning(false); // termina cannonThread

        while (retry)
        {
            try
            {
                cannonThread.join(); // espera cannonThread terminar
                retry = false;
            }
            catch (InterruptedException e)
            {
                Log.e(TAG, "Thread interrupted", e);
            }
        }
    } // fim do método surfaceDestroyed
            }
// chamado quando o usuário toca na tela nessa atividade
@Override
public boolean onTouchEvent(MotionEvent e)
{
    // obtém valor int representando o tipo de ação que causou esse evento
        int action = e.getAction();

        // o usuário tocou na tela ou arrastou o dedo pela tela
        if (action == motionEvent.ACTION_DOWN ||
        action == motionEvent.ACTION_MOVE)
        {
            fireCannonball(e);    // dispara a bala de canhão na direção do ponto do toque
        }

        return true;
} // fim do método onTouchEvent

// subclasse de Thread para controlar o loop do jogo
private class CannonThread extends Thread
{
    private SurfaceHolder surfaceHolder; // para manipular a tela de desenho
    private boolean threadIsRunning = true; // executando por padrão

    // inicializa holder de superfície
    public  CannonThread(SurfaceHolder holder)
    {
                surfaceHolder = holder;
                setName("CannonThread");
            }

    // altera o estado de execução
    public void setRunning(boolean running)
    {
        threadIsRunning = running;
    }

    // controla o loop do jogo
    @Override
    public void run()
    {
        Canvas canvas = null; // usado para desenhar
        long previousFrameTime = System.currentTimeMillis();

        while (threadIsRunning)
        {
            try
            {
                // obtém objeto Canvas para desenho exclusivo a partir dessa thread
                canvas = surfaceHolder.lockCanvas(null);

                // bloqueia surfaceHolder para desenhar
                synchronized(surfaceHolder)
                {
                    long currentTime = System.currentTimeMillis();
                    double elapsedTimeMS = currentTime - previousFrameTime;
                    totalElapsedTime += elapsedTimeMS / 1000.0;
                    updatePositions(elapsedTimeMS); // atualiza o estado do jogo drawGameElements(canvas);
                    drawGameElements(canvas);// desenha usando a tela de desenho
                    previousFrameTime = currentTime; // atualiza o tempo anterior
                    }
            }
            finally {
// exibe o conteúdo da tela de desenho no componente CannonView
// e permite que outras threads utilizem o objeto Canvas
                if (canvas != null)
                    surfaceHolder.unlockCanvasAndPost(canvas);
            }
        } // fim de while
        } // fim do método run
        } // fim da classe aninhada CannonThread
} // fim da classe CannonView