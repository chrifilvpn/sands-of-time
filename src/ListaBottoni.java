import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import java.util.ArrayList;

/**
 * Gruppo di bottoni utilizzabile sia con il mouse sia con la tastiera
 * (FRECCE SU/GIÙ o W/S per scegliere, INVIO o SPAZIO per confermare).
 *
 * I bottoni scattano al CLICK (pressione + rilascio), non appena si preme il mouse.
 * Un bottone disattivato (es. "Continua" senza salvataggio) appare grigio e viene saltato.
 */
public class ListaBottoni
{
    private static TextButton.TextButtonStyle stileNormale, stileSelezionato, stileDisabilitato;

    private final Stage stage;
    private final ArrayList<TextButton> bottoni = new ArrayList<TextButton>();
    private final ArrayList<Runnable> azioni = new ArrayList<Runnable>();
    private final ArrayList<Boolean> attivi = new ArrayList<Boolean>();
    private int selezionato = 0;
    private boolean visibile = true;

    public ListaBottoni(Stage stage)
    {
        this.stage = stage;
        creaStili();
    }

    private static void creaStili()
    {
        if (stileNormale != null) return;
        stileNormale = new TextButton.TextButtonStyle();
        stileNormale.up = Risorse.bottoneNormale;
        stileNormale.font = Risorse.fontNormale;
        stileNormale.fontColor = new Color(1f, 0.85f, 0.4f, 1f);

        stileSelezionato = new TextButton.TextButtonStyle();
        stileSelezionato.up = Risorse.bottoneSelezionato;
        stileSelezionato.font = Risorse.fontNormale;
        stileSelezionato.fontColor = Color.WHITE;

        stileDisabilitato = new TextButton.TextButtonStyle();
        stileDisabilitato.up = Risorse.bottoneDisabilitato;
        stileDisabilitato.font = Risorse.fontNormale;
        stileDisabilitato.fontColor = Color.GRAY;
    }

    /** Aggiunge un bottone. Le coordinate sono quelle dell'angolo in basso a sinistra. */
    public TextButton aggiungi(String testo, float x, float y, float w, float h, Runnable azione)
    {
        final int indice = bottoni.size();
        TextButton b = new TextButton(testo, stileNormale);
        b.setSize(w, h);
        b.setPosition(x, y);
        stage.addActor(b);

        b.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float px, float py) {
                if (visibile && attivi.get(indice))
                    esegui(indice);
            }

            @Override
            public void enter(InputEvent event, float px, float py, int pointer, Actor fromActor) {
                super.enter(event, px, py, pointer, fromActor);
                if (visibile && attivi.get(indice) && selezionato != indice) {
                    selezionato = indice;
                    aggiornaAspetto();
                    Risorse.suona(Risorse.sSeleziona);
                }
            }
        });

        bottoni.add(b);
        azioni.add(azione);
        attivi.add(true);
        if (bottoni.size() == 1) selezionato = 0;
        aggiornaAspetto();
        return b;
    }

    public void setAttivo(int indice, boolean attivo)
    {
        attivi.set(indice, attivo);
        if (!attivo && selezionato == indice)
            sposta(1);
        aggiornaAspetto();
    }

    public void setSelezionato(int indice)
    {
        if (attivi.get(indice)) {
            selezionato = indice;
            aggiornaAspetto();
        }
    }

    public void setVisibile(boolean v)
    {
        visibile = v;
        for (TextButton b : bottoni) {
            b.setVisible(v);
            if (v) b.toFront();
        }
    }

    public boolean isVisibile()
    {
        return visibile;
    }

    /** Da chiamare nel keyDown della schermata. Restituisce true se il tasto è stato usato. */
    public boolean gestisciTasto(int keycode)
    {
        if (!visibile || bottoni.isEmpty()) return false;
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            sposta(-1);
            Risorse.suona(Risorse.sSeleziona);
            return true;
        }
        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            sposta(1);
            Risorse.suona(Risorse.sSeleziona);
            return true;
        }
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            if (attivi.get(selezionato))
                esegui(selezionato);
            return true;
        }
        return false;
    }

    private void esegui(int indice)
    {
        Risorse.suona(Risorse.sClick);
        azioni.get(indice).run();
    }

    private void sposta(int direzione)
    {
        int n = bottoni.size();
        for (int i = 1; i <= n; i++) {
            int prossimo = ((selezionato + direzione * i) % n + n) % n;
            if (attivi.get(prossimo)) {
                selezionato = prossimo;
                break;
            }
        }
        aggiornaAspetto();
    }

    private void aggiornaAspetto()
    {
        for (int i = 0; i < bottoni.size(); i++) {
            TextButton.TextButtonStyle s;
            if (!attivi.get(i)) s = stileDisabilitato;
            else if (i == selezionato) s = stileSelezionato;
            else s = stileNormale;
            bottoni.get(i).setStyle(s);
        }
    }
}
