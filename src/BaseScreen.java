import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 *  Schermata di base: contiene due Stage (mondo di gioco e interfaccia).
 *
 *  Estende InputAdapter (invece di implementare InputProcessor a mano), così il codice
 *  compila sia con le versioni vecchie di libGDX sia con quelle nuove (il metodo scrolled è cambiato).
 *
 *  initialize() viene chiamato la prima volta che la schermata diventa visibile (show),
 *  quindi le sottoclassi possono ricevere parametri nel costruttore prima dell'inizializzazione.
 */
public abstract class BaseScreen extends InputAdapter implements Screen
{
    /** Dimensioni del mondo visibile: tutta l'interfaccia usa queste, mai la dimensione della finestra. */
    public static final float LARGHEZZA = 800;
    public static final float ALTEZZA = 480;

    protected Stage mainStage;
    protected Stage uiStage;
    protected Table uiTable;
    private Viewport mainViewport;
    private Viewport uiViewport;
    private boolean inizializzata = false;

    public BaseScreen()
    {
        mainViewport = new FitViewport(LARGHEZZA, ALTEZZA);
        uiViewport = new FitViewport(LARGHEZZA, ALTEZZA);

        mainStage = new Stage(mainViewport);
        uiStage = new Stage(uiViewport);

        uiTable = new Table();
        uiTable.setFillParent(true);
        uiStage.addActor(uiTable);
    }

    public abstract void initialize();

    public abstract void update(float dt);

    /**
     *  Se restituisce true, gli attori del mondo di gioco non vengono aggiornati
     *  (usato per la pausa: le animazioni si fermano davvero).
     */
    protected boolean isMondoFermo()
    {
        return false;
    }

    // Gameloop:
    // (1) input (eventi gestiti dai listener; input continuo in update)
    // (2) logica del gioco
    // (3) disegno
    @Override
    public void render(float dt)
    {
        // limita il tempo che può passare (es. mentre si trascina la finestra)
        dt = Math.min(dt, 1/30f);

        uiStage.act(dt);
        if (!isMondoFermo())
            mainStage.act(dt);

        update(dt);

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mainStage.getViewport().apply();
        mainStage.draw();
        uiStage.getViewport().apply();
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height)
    {
        mainViewport.update(width, height, true);
        uiViewport.update(width, height, true);
    }

    @Override
    public void pause()   {  }

    @Override
    public void resume()  {  }

    /** Libera la memoria degli stage (ogni Stage ha il suo SpriteBatch). */
    @Override
    public void dispose()
    {
        mainStage.dispose();
        uiStage.dispose();
    }

    /**
     *  Chiamato quando la schermata diventa attiva.
     *  Alla prima attivazione esegue initialize().
     */
    @Override
    public void show()
    {
        if (!inizializzata) {
            inizializzata = true;
            initialize();
        }
        InputMultiplexer im = (InputMultiplexer)Gdx.input.getInputProcessor();
        im.addProcessor(this);
        im.addProcessor(uiStage);
        im.addProcessor(mainStage);
    }

    /**
     *  Chiamato quando la schermata non è più attiva: smette di ricevere input.
     */
    @Override
    public void hide()
    {
        InputMultiplexer im = (InputMultiplexer)Gdx.input.getInputProcessor();
        im.removeProcessor(this);
        im.removeProcessor(uiStage);
        im.removeProcessor(mainStage);
    }
}
