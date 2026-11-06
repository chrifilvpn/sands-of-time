import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Schermata finale: congratulazioni, statistiche della partita e crediti.
 * (Prima c'era solo la scritta "FINE".)
 */
public class FineScreen extends BaseScreen
{
    private Image nero;
    private boolean pronto = false, uscendo = false;
    private Particelle scintille;
    private float timer = 0;

    @Override
    public void initialize()
    {
        Image sfondo = new Image(BaseActor.getTexture("assets/StanzaTesoro.png"));
        sfondo.setSize(LARGHEZZA, ALTEZZA);
        sfondo.setColor(0.35f, 0.3f, 0.25f, 1f);   // scurito per leggere il testo
        sfondo.setTouchable(Touchable.disabled);
        uiStage.addActor(sfondo);

        scintille = new Particelle(uiStage);
        scintille.avviaAmbiente(60, LARGHEZZA, ALTEZZA, new Color(1f, 0.85f, 0.4f, 1f));

        Color oro = new Color(1f, 0.82f, 0.35f, 1f);
        aggiungi("CONGRATULAZIONI!", Risorse.fontTitolo, oro, 370, 0.3f);
        aggiungi("Hai trovato il tesoro della piramide", Risorse.fontNormale, Color.WHITE, 322, 0.9f);
        aggiungi("TEMPO   " + StatoPartita.tempoFormattato(), Risorse.fontMedio, oro, 254, 1.5f);
        aggiungi("MORTI   " + StatoPartita.morti, Risorse.fontMedio, oro, 222, 1.8f);
        aggiungi("Un gioco del Gruppo Dugongo", Risorse.fontPiccolo, new Color(0.9f, 0.85f, 0.75f, 1f), 150, 2.4f);

        Label invio = aggiungi("Premi INVIO per tornare al menu", Risorse.fontPiccolo, Color.WHITE, 60, 3.2f);
        invio.addAction(Actions.sequence(Actions.delay(3.8f), Actions.forever(
                Actions.sequence(Actions.alpha(0.3f, 0.6f), Actions.alpha(1f, 0.6f)))));

        nero = new Image(Risorse.bianco);
        nero.setSize(LARGHEZZA, ALTEZZA);
        nero.setColor(0, 0, 0, 1);
        nero.setTouchable(Touchable.disabled);
        uiStage.addActor(nero);
        nero.addAction(Actions.sequence(Actions.fadeOut(1.2f), Actions.delay(2f), Actions.run(new Runnable() {
            public void run() { pronto = true; }
        })));

        sfondo.setTouchable(Touchable.enabled);
        sfondo.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { torna(); }
        });

        Risorse.musica(false);
    }

    private Label aggiungi(String testo, BitmapFont font, Color colore, float y, float ritardo)
    {
        Label l = Risorse.etichetta(testo, font, colore);
        l.setPosition(Math.round(LARGHEZZA / 2f - l.getWidth() / 2f), y);
        l.getColor().a = 0;
        l.setTouchable(Touchable.disabled);
        l.addAction(Actions.sequence(Actions.delay(ritardo), Actions.fadeIn(0.8f)));
        uiStage.addActor(l);
        return l;
    }

    private void torna()
    {
        if (!pronto || uscendo) return;
        uscendo = true;
        Risorse.suona(Risorse.sClick);
        nero.clearActions();
        nero.toFront();
        nero.addAction(Actions.sequence(Actions.fadeIn(0.6f), Actions.run(new Runnable() {
            public void run() { BaseGame.setActiveScreen(new MenuScreen()); }
        })));
    }

    @Override
    public boolean keyDown(int keycode)
    {
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE || keycode == Input.Keys.ESCAPE) {
            torna();
            return true;
        }
        return false;
    }

    @Override
    public void update(float dt)
    {
        // ogni tanto uno scoppio di scintille dorate
        timer += dt;
        if (timer > 0.7f) {
            timer = 0;
            scintille.esplosione(com.badlogic.gdx.math.MathUtils.random(100f, 700f),
                    com.badlogic.gdx.math.MathUtils.random(200f, 420f), 18,
                    new Color(1f, 0.85f, 0.4f, 0.9f), 90f, 1.0f, 60f);
        }
    }
}
