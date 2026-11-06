import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import java.util.ArrayList;

/**
 *  Creata all'avvio del programma: gestisce le schermate del gioco.
 *
 *  Novità rispetto alla versione originale:
 *  - quando si cambia schermata, quella vecchia viene distrutta (dispose) alla fine del frame,
 *    così texture, mappe, stage e musica non restano in memoria (prima la RAM cresceva a ogni livello);
 *  - F11 attiva/disattiva lo schermo intero, M toglie/rimette la musica (validi in tutte le schermate).
 */
public abstract class BaseGame extends Game
{
    /** Riferimento statico al gioco, usato da setActiveScreen. */
    private static BaseGame game;

    /** Schermate da distruggere al termine del frame corrente. */
    private final ArrayList<Screen> daDistruggere = new ArrayList<Screen>();

    public BaseGame()
    {
        game = this;
    }

    @Override
    public void create()
    {
        InputMultiplexer im = new InputMultiplexer();
        Gdx.input.setInputProcessor( im );

        // tasti globali, sempre attivi
        im.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.F11) {
                    alternaSchermoIntero();
                    return true;
                }
                if (keycode == Input.Keys.M) {
                    Risorse.alternaMuto();
                    return true;
                }
                return false;
            }
        });
    }

    private static void alternaSchermoIntero()
    {
        if (Gdx.graphics.isFullscreen())
            Gdx.graphics.setWindowedMode(800, 480);
        else
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    /**
     *  Cambia la schermata attiva. La schermata precedente viene distrutta
     *  alla fine del frame (non subito, perché potrebbe essere ancora in esecuzione).
     */
    public static void setActiveScreen(BaseScreen s)
    {
        Screen vecchia = game.getScreen();
        game.setScreen(s);
        if (vecchia != null && vecchia != s && !game.daDistruggere.contains(vecchia))
            game.daDistruggere.add(vecchia);
    }

    @Override
    public void render()
    {
        super.render();
        // ora la vecchia schermata ha finito il suo ultimo frame: si può liberare la memoria
        if (!daDistruggere.isEmpty()) {
            for (Screen s : daDistruggere)
                s.dispose();
            daDistruggere.clear();
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (getScreen() != null)
            getScreen().dispose();
        for (Screen s : daDistruggere)
            s.dispose();
        daDistruggere.clear();
    }
}
