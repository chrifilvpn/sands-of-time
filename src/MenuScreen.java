import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.math.Interpolation;

/**
 * Menu principale.
 * - L'immagine di sfondo non è più schiacciata (prima un'immagine 5:4 veniva stirata in 5:3).
 * - "CONTINUA" è disattivato se non c'è un salvataggio, e mostra il livello salvato.
 * - Si usa con mouse o tastiera (FRECCE + INVIO).
 */
public class MenuScreen extends BaseScreen
{
    private ListaBottoni bottoni;
    private Image nero;
    private boolean uscendo = false;

    @Override
    public void initialize()
    {
        // ---- sfondo ritagliato in proporzione 5:3 (senza deformarlo) ----
        // l'immagine è 1600x1280: ne prendiamo una fascia 1600x960 che include il titolo
        TextureRegion ritaglio = new TextureRegion(Risorse.sfondoMenu, 0, 70, 1600, 960);
        Image sfondo = new Image(ritaglio);
        sfondo.setSize(LARGHEZZA, ALTEZZA);
        sfondo.setOrigin(LARGHEZZA / 2f, ALTEZZA * 0.4f);
        sfondo.setTouchable(Touchable.disabled);
        uiStage.addActor(sfondo);
        // leggero zoom lento avanti e indietro: lo sfondo "respira"
        sfondo.addAction(Actions.forever(Actions.sequence(
                Actions.scaleTo(1.04f, 1.04f, 9f, Interpolation.sine),
                Actions.scaleTo(1f, 1f, 9f, Interpolation.sine))));

        // granelli di sabbia nel vento
        Particelle sabbia = new Particelle(uiStage);
        sabbia.avviaAmbiente(70, LARGHEZZA, ALTEZZA, new Color(1f, 0.85f, 0.55f, 1f));

        // pannello scuro dietro ai bottoni per leggerli meglio
        Image pannello = new Image(Risorse.pannello);
        if (Risorse.isWeb())
            pannello.setBounds(462, 94, 296, 136);
        else
            pannello.setBounds(462, 40, 296, 190);
        pannello.setColor(1, 1, 1, 0.9f);
        pannello.setTouchable(Touchable.disabled);
        uiStage.addActor(pannello);

        boolean salvataggio = Save.esiste();
        String testoContinua = salvataggio ? "CONTINUA  (LIV. " + Save.caricaLivello() + ")" : "CONTINUA";

        bottoni = new ListaBottoni(uiStage);
        bottoni.aggiungi(testoContinua, 480, 166, 260, 44, new Runnable() {
            public void run() { continua(); }
        });
        bottoni.aggiungi("NUOVA PARTITA", 480, 112, 260, 44, new Runnable() {
            public void run() { nuovaPartita(); }
        });
        if (!Risorse.isWeb()) {   // nel browser non si può "chiudere" il gioco
            bottoni.aggiungi("CHIUDI GIOCO", 480, 58, 260, 44, new Runnable() {
                public void run() { Gdx.app.exit(); }
            });
        }
        if (!salvataggio) {
            bottoni.setAttivo(0, false);
            bottoni.setSelezionato(1);
        }

        Label crediti = Risorse.etichetta("Gruppo Dugongo", Risorse.fontPiccolo, new Color(1f, 0.9f, 0.7f, 0.8f));
        crediti.setPosition(12, 10);
        uiStage.addActor(crediti);

        Label aiuto = Risorse.etichetta("FRECCE + INVIO   M audio   F11 schermo intero",
                Risorse.fontPiccolo, new Color(1f, 1f, 1f, 0.6f));
        aiuto.setPosition(LARGHEZZA - aiuto.getWidth() - 12, 10);
        uiStage.addActor(aiuto);

        nero = new Image(Risorse.bianco);
        nero.setSize(LARGHEZZA, ALTEZZA);
        nero.setColor(0, 0, 0, 1);
        nero.setTouchable(Touchable.disabled);
        uiStage.addActor(nero);
        nero.addAction(Actions.fadeOut(0.8f));

        Risorse.musica(false);
    }

    private void continua()
    {
        StatoPartita.daSalvataggio();
        vai(new LevelScreen(Save.caricaLivello(), Save.caricaArtefatto(), Save.caricaPassato()));
    }

    private void nuovaPartita()
    {
        // bug corretto: prima artefatto ed epoca della partita precedente restavano attivi
        StatoPartita.nuova();
        Save.cancella();
        vai(new LevelScreen(1, false, false));
    }

    private void vai(final BaseScreen schermata)
    {
        if (uscendo) return;
        uscendo = true;
        nero.clearActions();
        nero.toFront();
        nero.addAction(Actions.sequence(Actions.fadeIn(0.5f), Actions.run(new Runnable() {
            public void run() { BaseGame.setActiveScreen(schermata); }
        })));
    }

    @Override
    public boolean keyDown(int keycode)
    {
        // nel browser l'audio può partire solo dopo che il giocatore ha premuto qualcosa
        Risorse.musica(false);
        if (uscendo) return false;
        return bottoni.gestisciTasto(keycode);
    }

    @Override
    public void update(float dt)
    {
        if (Gdx.input.justTouched())
            Risorse.musica(false);
    }
}
