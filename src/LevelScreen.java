import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.ArrayList;
import java.util.List;

/**
 * Schermata di un livello.
 *
 * Ogni livello è formato da mappaN_presente.tmx e (facoltativo) mappaN_passato.tmx.
 * Oggetti letti dalle mappe (per nome):
 *   solid      muri e pavimenti
 *   kill       trappole (morte al contatto)
 *   finish     uscita del livello
 *   start      punto di partenza
 *   artefatto  (o "object") oggetto da ispezionare per ottenere l'artefatto del tempo
 *
 * Proprietà della mappa del presente (Mappa > Proprietà mappa in Tiled):
 *   dimensionePersonaggio  dimensione in pixel del personaggio (default 57.6)
 *   uscita                 "tocco"   = si esce toccando "finish" (default)
 *                          "porta"   = si esce premendo E davanti a "finish"
 *                          "passato" = nel presente "finish" è un muro, si esce solo nel passato
 *                          "tesoro"  = premendo E davanti a "finish" il gioco finisce
 *   titolo                 nome del livello mostrato all'inizio
 */
public class LevelScreen extends BaseScreen
{
    // ------------------------------------------------ parametri
    private final int livello;
    private boolean artefattoAlRiavvio;
    private final boolean passatoIniziale;

    // ------------------------------------------------ stato
    private boolean hasArtifact;
    private boolean isInPassato;
    private boolean pausa, gameOver, transizione, cutscene, cutsceneChiudibile;
    private float cooldownTempo = 0f;
    private float tremolio = 0f;
    private float tempoAnimazione = 0f;
    private float camX, camY;
    private String tipoUscita;

    // ------------------------------------------------ mondo
    private TilemapActor mappaPresente, mappaPassato;
    private Esploratore esploratore;
    private final ArrayList<Oggetto> solidiPresente   = new ArrayList<Oggetto>();
    private final ArrayList<Oggetto> solidiPassato    = new ArrayList<Oggetto>();
    private final ArrayList<Oggetto> trappolePresente = new ArrayList<Oggetto>();
    private final ArrayList<Oggetto> trappolePassato  = new ArrayList<Oggetto>();
    private Oggetto uscitaPresente, uscitaPassato, oggettoArtefatto;
    private Label etichettaInterazione;
    private Particelle particelleMondo, polvere;
    private boolean eraATerra = true;
    private float timerScintille = 0f;

    private static final Color SABBIA = new Color(0.95f, 0.8f, 0.55f, 0.9f);
    private static final Color POLVERE_PRESENTE = new Color(1f, 0.85f, 0.6f, 1f);
    private static final Color POLVERE_PASSATO = new Color(0.6f, 0.9f, 1f, 1f);
    private static final Color ORO = new Color(1f, 0.85f, 0.35f, 0.9f);
    private static final Color AZZURRO = new Color(0.55f, 0.9f, 1f, 0.9f);

    // ------------------------------------------------ interfaccia
    private Image vignetta, flash, nero;
    private Image iconaArtefatto;
    private Label tastoT, hudLivello, hudEpoca, messaggio, titoloLivello, sottotitoloLivello;
    private Image ingranaggio;

    private Image cutOverlay, cutImmagine;
    private Label cutTitolo, cutSotto;

    private Image pausaOverlay, pausaPannello;
    private Label pausaTitolo;
    private final ArrayList<Label> pausaAiuto = new ArrayList<Label>();
    private Image messaggioSfondo;
    private ListaBottoni pausaBottoni;

    private Image goOverlay;
    private Label goTitolo, goSotto;
    private ListaBottoni goBottoni;

    public LevelScreen(int livello, boolean hasArtifact, boolean passato)
    {
        this.livello = livello;
        this.artefattoAlRiavvio = hasArtifact;
        this.passatoIniziale = passato;
    }

    // =====================================================================
    // Creazione del livello
    // =====================================================================

    @Override
    public void initialize()
    {
        hasArtifact = artefattoAlRiavvio;

        // ---------- mappe ----------
        mappaPresente = new TilemapActor("assets/mappa" + livello + "_presente.tmx", mainStage);
        String filePassato = "assets/mappa" + livello + "_passato.tmx";
        if (Gdx.files.internal(filePassato).exists())
            mappaPassato = new TilemapActor(filePassato, mainStage);

        tipoUscita = mappaPresente.getProprietaTesto("uscita", "tocco");
        float dimensione = mappaPresente.getProprietaNumero("dimensionePersonaggio", 57.6f);

        // ---------- oggetti ----------
        uscitaPresente = caricaOggetti(mappaPresente, solidiPresente, trappolePresente);
        if (mappaPassato != null)
            uscitaPassato = caricaOggetti(mappaPassato, solidiPassato, trappolePassato);

        // nel livello con uscita "passato" la porta del presente è chiusa: diventa un muro
        if (tipoUscita.equals("passato") && uscitaPresente != null) {
            Rectangle r = uscitaPresente.getRettangolo();
            solidiPresente.add(new Oggetto(r.x, r.y, r.width, r.height, Oggetto.Tipo.SOLIDO, mainStage));
            uscitaPresente.remove();
            uscitaPresente = null;
        }

        MapObject art = primo(mappaPresente, "artefatto");
        if (art == null) art = primo(mappaPresente, "object");
        if (art != null)
            oggettoArtefatto = Oggetto.daMappa(art, Oggetto.Tipo.INTERAZIONE, mainStage);

        // ---------- epoca iniziale ----------
        // si parte nel passato solo se il livello HA una mappa del passato (bug corretto:
        // prima si poteva arrivare al livello 12 "nel passato" e cadere nel vuoto)
        boolean passato = passatoIniziale && mappaPassato != null;

        // ---------- personaggio ----------
        esploratore = new Esploratore(0, 0, mainStage, dimensione);
        MapObject start = primo(passato ? mappaPassato : mappaPresente, "start");
        if (start == null && mappaPassato != null)
            start = primo(passato ? mappaPresente : mappaPassato, "start");
        if (start != null) {
            Oggetto punto = Oggetto.daMappa(start, Oggetto.Tipo.INTERAZIONE, mainStage);
            Rectangle r = punto.getRettangolo();
            punto.remove();
            esploratore.posizionaPiedi(r.x + r.width / 2f, r.y);
        } else {
            esploratore.posizionaPiedi(100, 100);
        }

        particelleMondo = new Particelle(mainStage);

        etichettaInterazione = Risorse.etichetta("", Risorse.fontPiccolo, new Color(1f, 0.9f, 0.3f, 1f));
        etichettaInterazione.setVisible(false);
        mainStage.addActor(etichettaInterazione);

        creaInterfaccia();

        // ---------- avvio ----------
        setTempo(passato);
        aggiornaCamera(0, true);
        mostraTitoloLivello();
        if (livello == 1)
            mostraMessaggio("FRECCE o A/D per muoverti  -  SPAZIO per saltare", 6f);

        Save.salva(livello, hasArtifact, passato, StatoPartita.morti, StatoPartita.tempo);
    }

    /** Legge solidi, trappole e uscita di una mappa. Restituisce l'uscita (o null). */
    private Oggetto caricaOggetti(TilemapActor mappa, List<Oggetto> solidi, List<Oggetto> trappole)
    {
        for (MapObject obj : mappa.getObjectList("solid"))
            solidi.add(Oggetto.daMappa(obj, Oggetto.Tipo.SOLIDO, mainStage));
        for (MapObject obj : mappa.getObjectList("kill"))
            trappole.add(Oggetto.daMappa(obj, Oggetto.Tipo.TRAPPOLA, mainStage));
        MapObject fine = primo(mappa, "finish");
        return (fine == null) ? null : Oggetto.daMappa(fine, Oggetto.Tipo.USCITA, mainStage);
    }

    private static MapObject primo(TilemapActor mappa, String nome)
    {
        ArrayList<MapObject> l = mappa.getObjectList(nome);
        return l.isEmpty() ? null : l.get(0);
    }

    // =====================================================================
    // Interfaccia
    // =====================================================================

    private Image rettangolo(Color colore, float alpha)
    {
        Image img = new Image(Risorse.bianco);
        img.setSize(LARGHEZZA, ALTEZZA);
        img.setColor(colore.r, colore.g, colore.b, alpha);
        img.setTouchable(Touchable.disabled);
        uiStage.addActor(img);
        return img;
    }

    private Label etichettaUi(String testo, com.badlogic.gdx.graphics.g2d.BitmapFont font, Color colore)
    {
        Label l = Risorse.etichetta(testo, font, colore);
        l.setTouchable(Touchable.disabled);
        uiStage.addActor(l);
        return l;
    }

    private static void centra(Label l, float y)
    {
        l.pack();
        l.setPosition(Math.round(LARGHEZZA / 2f - l.getWidth() / 2f), Math.round(y));
    }

    private void creaInterfaccia()
    {
        Color oro = new Color(1f, 0.82f, 0.35f, 1f);

        // effetti a schermo intero
        vignetta = new Image(Risorse.vignetta);
        vignetta.setSize(LARGHEZZA, ALTEZZA);
        vignetta.setTouchable(Touchable.disabled);
        vignetta.getColor().a = 0;
        vignetta.setVisible(false);
        uiStage.addActor(vignetta);
        polvere = new Particelle(uiStage);
        polvere.avviaAmbiente(40, LARGHEZZA, ALTEZZA, POLVERE_PRESENTE);
        flash = rettangolo(Color.WHITE, 0f);

        // HUD in alto a sinistra
        hudLivello = etichettaUi("LIVELLO " + livello + "/" + Risorse.numeroLivelli, Risorse.fontPiccolo, oro);
        hudLivello.setPosition(12, ALTEZZA - 24);
        hudEpoca = etichettaUi("", Risorse.fontPiccolo, Color.WHITE);
        hudEpoca.setPosition(12, ALTEZZA - 42);
        hudEpoca.setVisible(mappaPassato != null);

        // artefatto in basso a destra (luminoso nel passato)
        iconaArtefatto = new Image(Risorse.artefattoSpento);
        iconaArtefatto.setSize(56, 56);
        iconaArtefatto.setPosition(LARGHEZZA - 56 - 12, 12);
        iconaArtefatto.setOrigin(28, 28);
        iconaArtefatto.setTouchable(Touchable.disabled);
        iconaArtefatto.setVisible(hasArtifact);
        uiStage.addActor(iconaArtefatto);
        tastoT = etichettaUi("[T]", Risorse.fontPiccolo, oro);
        tastoT.setPosition(LARGHEZZA - 56 - 12 - tastoT.getWidth() - 6, 20);
        tastoT.setVisible(hasArtifact && mappaPassato != null);

        // ingranaggio (pausa) in alto a destra
        ingranaggio = new Image(Risorse.ingranaggio);
        ingranaggio.setSize(36, 36);
        ingranaggio.setOrigin(18, 18);
        ingranaggio.setPosition(LARGHEZZA - 36 - 10, ALTEZZA - 36 - 10);
        ingranaggio.setColor(1, 1, 1, 0.7f);
        uiStage.addActor(ingranaggio);
        ingranaggio.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (puoMettereInPausa()) alternaPausa();
            }
        });

        // titolo del livello e messaggi
        titoloLivello = etichettaUi("LIVELLO " + livello, Risorse.fontTitolo, oro);
        centra(titoloLivello, ALTEZZA * 0.62f);
        titoloLivello.getColor().a = 0;
        sottotitoloLivello = etichettaUi(mappaPresente.getProprietaTesto("titolo", "").toUpperCase(),
                Risorse.fontMedio, Color.WHITE);
        centra(sottotitoloLivello, ALTEZZA * 0.62f - 34);
        sottotitoloLivello.getColor().a = 0;

        messaggioSfondo = rettangolo(Color.BLACK, 0f);
        messaggio = etichettaUi("", Risorse.fontNormale, Color.WHITE);
        messaggio.getColor().a = 0;

        // ---------- cutscene artefatto ----------
        cutOverlay = rettangolo(Color.BLACK, 0f);
        cutOverlay.setVisible(false);
        cutTitolo = etichettaUi("Hai trovato un artefatto misterioso", Risorse.fontMedio, oro);
        centra(cutTitolo, ALTEZZA * 0.74f);
        cutTitolo.setVisible(false);
        cutImmagine = new Image(Risorse.artefattoAcceso);
        cutImmagine.setSize(150, 150);
        cutImmagine.setOrigin(75, 75);
        cutImmagine.setPosition(LARGHEZZA / 2f - 75, ALTEZZA * 0.30f);
        cutImmagine.setTouchable(Touchable.disabled);
        cutImmagine.setVisible(false);
        uiStage.addActor(cutImmagine);
        cutSotto = etichettaUi("Premi E per continuare", Risorse.fontPiccolo, Color.WHITE);
        centra(cutSotto, ALTEZZA * 0.17f);
        cutSotto.setVisible(false);

        // ---------- pausa ----------
        pausaOverlay = rettangolo(Color.BLACK, 0.6f);
        pausaPannello = new Image(Risorse.pannello);
        pausaPannello.setBounds(200, 40, 400, 400);
        pausaPannello.setTouchable(Touchable.disabled);
        uiStage.addActor(pausaPannello);
        pausaTitolo = etichettaUi("PAUSA", Risorse.fontGrande, oro);
        centra(pausaTitolo, 395);
        pausaBottoni = new ListaBottoni(uiStage);
        pausaBottoni.aggiungi("RIPRENDI",         270, 330, 260, 36, new Runnable() { public void run() { alternaPausa(); } });
        pausaBottoni.aggiungi("RICOMINCIA LIVELLO", 270, 286, 260, 36, new Runnable() { public void run() { riprova(); } });
        pausaBottoni.aggiungi("MENU PRINCIPALE",  270, 242, 260, 36, new Runnable() { public void run() { vaiAlMenu(); } });
        if (!Risorse.isWeb())
            pausaBottoni.aggiungi("CHIUDI GIOCO", 270, 198, 260, 36, new Runnable() { public void run() { Gdx.app.exit(); } });
        // elenco dei comandi: una riga per etichetta, così l'interlinea è sempre corretta
        String[][] comandi = {
                {"FRECCE / A D", "muoviti"},
                {"SPAZIO / W", "salta"},
                {"E", "interagisci"},
                {"T", "viaggia nel tempo"},
                {"ESC / P", "pausa"},
                {"M", "audio on/off"},
                {"F11", "schermo intero"}
        };
        Color coloreTasto = new Color(1f, 0.82f, 0.35f, 1f);
        Color coloreDescr = new Color(0.9f, 0.85f, 0.75f, 1f);
        float yRiga = Risorse.isWeb() ? 200 : 168;
        for (String[] c : comandi) {
            Label tasto = etichettaUi(c[0], Risorse.fontPiccolo, coloreTasto);
            tasto.setPosition(250, yRiga);
            Label descr = etichettaUi(c[1], Risorse.fontPiccolo, coloreDescr);
            descr.setPosition(400, yRiga);
            pausaAiuto.add(tasto);
            pausaAiuto.add(descr);
            yRiga -= 17;
        }
        mostraPausa(false);

        // ---------- game over ----------
        goOverlay = rettangolo(new Color(0.15f, 0f, 0f, 1f), 0.8f);
        goTitolo = etichettaUi("GAME OVER", Risorse.fontTitolo, new Color(1f, 0.25f, 0.2f, 1f));
        centra(goTitolo, 300);
        goSotto = etichettaUi("", Risorse.fontPiccolo, new Color(0.9f, 0.8f, 0.8f, 1f));
        goBottoni = new ListaBottoni(uiStage);
        goBottoni.aggiungi("RIPROVA",         280, 200, 240, 38, new Runnable() { public void run() { riprova(); } });
        goBottoni.aggiungi("MENU PRINCIPALE", 280, 152, 240, 38, new Runnable() { public void run() { vaiAlMenu(); } });
        mostraGameOverUi(false);

        // dissolvenza dal nero all'avvio
        nero = rettangolo(Color.BLACK, 1f);
        nero.addAction(Actions.fadeOut(0.5f));
    }

    private void mostraTitoloLivello()
    {
        titoloLivello.addAction(Actions.sequence(Actions.delay(0.3f), Actions.fadeIn(0.5f),
                Actions.delay(1.6f), Actions.fadeOut(0.6f)));
        sottotitoloLivello.addAction(Actions.sequence(Actions.delay(0.5f), Actions.fadeIn(0.5f),
                Actions.delay(1.4f), Actions.fadeOut(0.6f)));
    }

    /** Messaggio temporaneo in basso al centro. */
    private void mostraMessaggio(String testo, float secondi)
    {
        // in alto, su una striscia scura: non copre più il personaggio
        messaggio.setText(testo);
        centra(messaggio, ALTEZZA - 92);
        messaggioSfondo.setBounds(messaggio.getX() - 14, messaggio.getY() - 9,
                messaggio.getWidth() + 28, messaggio.getHeight() + 18);
        messaggio.clearActions();
        messaggio.getColor().a = 0;
        messaggio.addAction(Actions.sequence(Actions.fadeIn(0.3f), Actions.delay(secondi), Actions.fadeOut(0.6f)));
        messaggioSfondo.clearActions();
        messaggioSfondo.getColor().a = 0;
        messaggioSfondo.addAction(Actions.sequence(Actions.alpha(0.55f, 0.3f), Actions.delay(secondi), Actions.fadeOut(0.6f)));
    }

    private void mostraPausa(boolean v)
    {
        pausaOverlay.setVisible(v);
        pausaPannello.setVisible(v);
        pausaTitolo.setVisible(v);
        for (Label l : pausaAiuto) l.setVisible(v);
        if (v) {
            pausaOverlay.toFront();
            pausaPannello.toFront();
            pausaTitolo.toFront();
            for (Label l : pausaAiuto) l.toFront();
            pausaBottoni.setSelezionato(0);
        }
        pausaBottoni.setVisibile(v);
    }

    private void mostraGameOverUi(boolean v)
    {
        goOverlay.setVisible(v);
        goTitolo.setVisible(v);
        goSotto.setVisible(v);
        if (v) {
            goOverlay.toFront();
            goTitolo.toFront();
            goSotto.toFront();
            goBottoni.setSelezionato(0);
        }
        goBottoni.setVisibile(v);
    }

    // =====================================================================
    // Viaggio nel tempo
    // =====================================================================

    private void setTempo(boolean passato)
    {
        isInPassato = passato;

        for (Oggetto o : solidiPresente)   o.setEnable(!passato);
        for (Oggetto o : trappolePresente) o.setEnable(!passato);
        for (Oggetto o : solidiPassato)    o.setEnable(passato);
        for (Oggetto o : trappolePassato)  o.setEnable(passato);
        if (uscitaPresente != null) uscitaPresente.setEnable(!passato);
        if (uscitaPassato != null)  uscitaPassato.setEnable(passato);

        mappaPresente.setVisible(!passato);
        if (mappaPassato != null) mappaPassato.setVisible(passato);

        vignetta.clearActions();
        if (passato) {
            vignetta.setVisible(true);
            vignetta.addAction(Actions.fadeIn(0.3f));
        } else {
            vignetta.addAction(Actions.sequence(Actions.fadeOut(0.3f), Actions.visible(false)));
        }

        Texture icona = passato ? Risorse.artefattoAcceso : Risorse.artefattoSpento;
        iconaArtefatto.setDrawable(new TextureRegionDrawable(new TextureRegion(icona)));

        polvere.setColoreAmbiente(passato ? POLVERE_PASSATO : POLVERE_PRESENTE);

        hudEpoca.setText(passato ? "PASSATO" : "PRESENTE");
        hudEpoca.pack();
        hudEpoca.setColor(passato ? new Color(0.55f, 0.85f, 1f, 1f) : new Color(1f, 0.85f, 0.55f, 1f));

        Risorse.musica(passato);
    }

    private void viaggiaNelTempo()
    {
        if (!hasArtifact || mappaPassato == null || cooldownTempo > 0) return;
        boolean destinazione = !isInPassato;

        // se nell'altra epoca qui c'è un muro, il viaggio è bloccato (prima si restava incastrati)
        if (bloccato(destinazione)) {
            Risorse.suona(Risorse.sBloccato);
            lampo(new Color(1f, 0.2f, 0.1f, 1f), 0.35f, 0.25f);
            tremolio = 3f;
            cooldownTempo = 0.3f;
            mostraMessaggio("Qualcosa ti blocca nell'altra epoca!", 1.5f);
            return;
        }

        setTempo(destinazione);
        particelleMondo.esplosione(esploratore.getCentroX(), esploratore.getCentroY(), 36,
                destinazione ? AZZURRO : ORO, 140f, 0.7f, 0f);
        cooldownTempo = 0.45f;
        tremolio = 4f;
        Risorse.suona(Risorse.sTempo);
        lampo(destinazione ? new Color(0.6f, 0.85f, 1f, 1f) : new Color(1f, 0.85f, 0.5f, 1f), 0.75f, 0.45f);
        iconaArtefatto.clearActions();
        iconaArtefatto.setScale(1f);
        iconaArtefatto.addAction(Actions.sequence(Actions.scaleTo(1.3f, 1.3f, 0.1f), Actions.scaleTo(1f, 1f, 0.25f)));
    }

    private boolean bloccato(boolean passato)
    {
        Rectangle hb = esploratore.getHitbox(2f);
        for (Oggetto o : (passato ? solidiPassato : solidiPresente))
            if (hb.overlaps(o.getRettangolo()))
                return true;
        return false;
    }

    private void lampo(Color c, float alpha, float durata)
    {
        flash.clearActions();
        flash.setColor(c.r, c.g, c.b, alpha);
        flash.addAction(Actions.fadeOut(durata));
    }

    // =====================================================================
    // Ciclo di gioco
    // =====================================================================

    @Override
    protected boolean isMondoFermo()
    {
        return pausa || cutscene;
    }

    @Override
    public void update(float dt)
    {
        tempoAnimazione += dt;
        if (!pausa && !gameOver && !transizione)
            StatoPartita.tempo += dt;
        cooldownTempo -= dt;

        // morte: aspetta la fine dell'animazione, poi mostra il Game Over
        if (esploratore.isDead()) {
            if (!gameOver && esploratore.isDeathFinished())
                mostraGameOver();
            aggiornaCamera(dt, false);
            return;
        }

        if (pausa || cutscene || transizione || gameOver) {
            aggiornaCamera(dt, false);
            return;
        }

        List<Oggetto> solidi = isInPassato ? solidiPassato : solidiPresente;

        esploratore.leggiInput(dt);
        esploratore.applyGravity(dt);
        esploratore.moveX(dt);
        collisioni(solidi, true);
        esploratore.moveY(dt);
        collisioni(solidi, false);
        esploratore.restaNelMondo();
        if (esploratore.aggiornaStato(solidi))
            Risorse.suona(Risorse.sSalto);
        if (esploratore.isATerra() && !eraATerra)
            particelleMondo.sbuffo(esploratore.getCentroX(), esploratore.getPiediY(), SABBIA);
        eraATerra = esploratore.isATerra();
        emettiScintille(dt);

        // trappole dell'epoca attuale
        for (Oggetto k : (isInPassato ? trappolePassato : trappolePresente)) {
            if (esploratore.overlaps(k)) {
                muori();
                return;
            }
        }

        aggiornaInterazione();

        // uscita al contatto
        if (tipoUscita.equals("tocco") || tipoUscita.equals("passato")) {
            Oggetto u = uscitaAttiva();
            if (u != null && esploratore.overlaps(u))
                cambiaLivello();
        }

        aggiornaCamera(dt, false);
    }

    /**
     * Separa il personaggio dai solidi (prima in orizzontale, poi in verticale).
     * Stessa logica dell'originale, con una correzione: quando si salta contro un soffitto
     * il personaggio ora viene respinto verso il basso invece di "incollarsi".
     */
    private void collisioni(List<Oggetto> solidi, boolean orizzontale)
    {
        for (Oggetto o : solidi) {
            if (!o.isEnable() || !esploratore.overlaps(o)) continue;
            float yPrima = esploratore.getY();
            Vector2 spinta = esploratore.preventOverlap(o);
            if (spinta == null) continue;

            boolean spintoGiu = esploratore.getY() < yPrima;
            if (orizzontale) {
                if (spintoGiu) esploratore.setY(yPrima);
                else esploratore.velocityVec.x = 0;
            } else {
                if (spintoGiu && esploratore.velocityVec.y <= 0) esploratore.setY(yPrima);
                if (Math.abs(spinta.y) >= Math.abs(spinta.x)) esploratore.velocityVec.y = 0;
            }
        }
    }

    /** Luccichio sull'uscita e sull'oggetto con l'artefatto: aiuta a capire dove andare. */
    private void emettiScintille(float dt)
    {
        timerScintille -= dt;
        if (timerScintille > 0) return;
        timerScintille = 0.09f;
        Oggetto u = uscitaAttiva();
        if (u != null)
            particelleMondo.scintilla(u.getX(), u.getY(), u.getWidth(), u.getHeight() * 0.8f,
                    isInPassato ? AZZURRO : ORO);
        if (!hasArtifact && !isInPassato && oggettoArtefatto != null)
            particelleMondo.scintilla(oggettoArtefatto.getX(), oggettoArtefatto.getY(),
                    oggettoArtefatto.getWidth(), oggettoArtefatto.getHeight(), AZZURRO);
    }

    private Oggetto uscitaAttiva()
    {
        return isInPassato ? uscitaPassato : uscitaPresente;
    }

    /** Mostra "[E] ..." sopra la porta / il tesoro / l'artefatto quando ci sei vicino. */
    private void aggiornaInterazione()
    {
        Oggetto bersaglio = null;
        String testo = "";

        Oggetto u = uscitaAttiva();
        if (u != null && esploratore.overlaps(u)) {
            if (tipoUscita.equals("porta"))  { bersaglio = u; testo = "[E] ENTRA"; }
            if (tipoUscita.equals("tesoro")) { bersaglio = u; testo = "[E] APRI IL TESORO"; }
        }
        if (bersaglio == null && !hasArtifact && !isInPassato && oggettoArtefatto != null
                && esploratore.overlaps(oggettoArtefatto)) {
            bersaglio = oggettoArtefatto;
            testo = "[E] ISPEZIONA";
        }

        etichettaInterazione.setVisible(bersaglio != null);
        if (bersaglio != null) {
            etichettaInterazione.setText(testo);
            etichettaInterazione.pack();
            float bob = MathUtils.sin(tempoAnimazione * 4f) * 3f;
            etichettaInterazione.setPosition(
                    Math.round(bersaglio.getX() + bersaglio.getWidth() / 2f - etichettaInterazione.getWidth() / 2f),
                    Math.round(bersaglio.getY() + bersaglio.getHeight() + 10 + bob));
        }
    }

    private void interagisci()
    {
        Oggetto u = uscitaAttiva();
        if (u != null && esploratore.overlaps(u)) {
            if (tipoUscita.equals("porta"))  { cambiaLivello(); return; }
            if (tipoUscita.equals("tesoro")) { finale(); return; }
        }
        if (!hasArtifact && !isInPassato && oggettoArtefatto != null && esploratore.overlaps(oggettoArtefatto))
            mostraCutscene();
    }

    /** Telecamera che segue il personaggio in modo morbido, senza uscire dalla mappa. */
    private void aggiornaCamera(float dt, boolean subito)
    {
        Rectangle m = BaseActor.getWorldBounds();
        float tx = esploratore.getCentroX();
        float ty = esploratore.getCentroY();
        tx = (m.width  <= LARGHEZZA) ? m.width / 2f  : MathUtils.clamp(tx, LARGHEZZA / 2f, m.width - LARGHEZZA / 2f);
        ty = (m.height <= ALTEZZA)   ? m.height / 2f : MathUtils.clamp(ty, ALTEZZA / 2f, m.height - ALTEZZA / 2f);

        if (subito) {
            camX = tx;
            camY = ty;
        } else {
            float k = Math.min(1f, dt * 8f);
            camX += (tx - camX) * k;
            camY += (ty - camY) * k;
        }

        float ox = 0, oy = 0;
        if (tremolio > 0) {
            ox = MathUtils.random(-tremolio, tremolio);
            oy = MathUtils.random(-tremolio, tremolio);
            tremolio = Math.max(0, tremolio - dt * 15f);
        }

        Camera cam = mainStage.getCamera();
        cam.position.set(Math.round(camX + ox), Math.round(camY + oy), 0);
        cam.update();
    }

    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height);
        if (esploratore != null)
            aggiornaCamera(0, true);
    }

    // =====================================================================
    // Eventi: cutscene, morte, uscita, fine
    // =====================================================================

    private void mostraCutscene()
    {
        cutscene = true;
        cutsceneChiudibile = false;
        etichettaInterazione.setVisible(false);
        Risorse.suona(Risorse.sArtefatto);

        cutOverlay.setVisible(true);
        cutOverlay.getColor().a = 0;
        cutOverlay.addAction(Actions.alpha(0.8f, 0.6f));

        cutTitolo.setVisible(true);
        cutTitolo.getColor().a = 0;
        cutTitolo.addAction(Actions.fadeIn(1.0f));

        cutImmagine.setVisible(true);
        cutImmagine.getColor().a = 0;
        cutImmagine.setScale(0.6f);
        cutImmagine.addAction(Actions.sequence(
                Actions.delay(0.4f),
                Actions.parallel(Actions.fadeIn(1.2f), Actions.scaleTo(1f, 1f, 1.2f)),
                Actions.forever(Actions.sequence(Actions.scaleTo(1.06f, 1.06f, 0.8f), Actions.scaleTo(1f, 1f, 0.8f)))));

        cutSotto.setVisible(true);
        cutSotto.getColor().a = 0;
        cutSotto.addAction(Actions.sequence(Actions.delay(1.6f), Actions.run(new Runnable() {
            public void run() { cutsceneChiudibile = true; }
        }), Actions.forever(Actions.sequence(Actions.fadeIn(0.5f), Actions.fadeOut(0.5f)))));
    }

    private void chiudiCutscene()
    {
        cutscene = false;
        hasArtifact = true;
        artefattoAlRiavvio = true;
        for (com.badlogic.gdx.scenes.scene2d.Actor a :
                new com.badlogic.gdx.scenes.scene2d.Actor[] { cutOverlay, cutTitolo, cutImmagine, cutSotto }) {
            a.clearActions();
            a.setVisible(false);
        }

        iconaArtefatto.setVisible(true);
        iconaArtefatto.setScale(0f);
        iconaArtefatto.addAction(Actions.scaleTo(1f, 1f, 0.4f));
        tastoT.setVisible(mappaPassato != null);
        mostraMessaggio("Premi T per viaggiare nel tempo", 5f);
        Save.salva(livello, true, isInPassato, StatoPartita.morti, StatoPartita.tempo);
    }

    private void muori()
    {
        esploratore.morte();
        etichettaInterazione.setVisible(false);
        StatoPartita.morti++;
        Risorse.pausaMusica();
        Risorse.suona(Risorse.sMorte);
        tremolio = 6f;
        lampo(new Color(1f, 0.1f, 0.05f, 1f), 0.45f, 0.5f);
        particelleMondo.esplosione(esploratore.getCentroX(), esploratore.getCentroY(), 24,
                new Color(0.9f, 0.3f, 0.2f, 0.9f), 110f, 0.8f, 200f);
        Save.salva(livello, artefattoAlRiavvio, passatoIniziale && mappaPassato != null,
                StatoPartita.morti, StatoPartita.tempo);
    }

    private void mostraGameOver()
    {
        gameOver = true;
        goSotto.setText("Morti finora: " + StatoPartita.morti + "     (R = riprova)");
        centra(goSotto, 252);
        mostraGameOverUi(true);
        goOverlay.getColor().a = 0;
        goOverlay.addAction(Actions.alpha(0.8f, 0.4f));
        goTitolo.getColor().a = 0;
        goTitolo.addAction(Actions.fadeIn(0.5f));
        nero.toFront();
    }

    private void cambiaLivello()
    {
        if (transizione) return;
        etichettaInterazione.setVisible(false);
        Risorse.suona(Risorse.sPorta);
        final int prossimo = livello + 1;
        final boolean artefatto = hasArtifact;
        final boolean passato = isInPassato;
        if (prossimo > Risorse.numeroLivelli) {
            finale();
            return;
        }
        dissolviVerso(0.6f, new Runnable() {
            public void run() { BaseGame.setActiveScreen(new LevelScreen(prossimo, artefatto, passato)); }
        });
    }

    private void finale()
    {
        if (transizione) return;
        etichettaInterazione.setVisible(false);
        Risorse.suona(Risorse.sVittoria);
        Save.cancella();
        flash.clearActions();
        flash.setColor(1f, 0.9f, 0.6f, 0f);
        flash.addAction(Actions.alpha(1f, 1.2f));
        dissolviVerso(1.8f, new Runnable() {
            public void run() { BaseGame.setActiveScreen(new FineScreen()); }
        });
    }

    private void riprova()
    {
        if (transizione) return;
        dissolviVerso(0.3f, new Runnable() {
            public void run() { BaseGame.setActiveScreen(new LevelScreen(livello, artefattoAlRiavvio, passatoIniziale)); }
        });
    }

    private void vaiAlMenu()
    {
        if (transizione) return;
        dissolviVerso(0.3f, new Runnable() {
            public void run() { BaseGame.setActiveScreen(new MenuScreen()); }
        });
    }

    private void dissolviVerso(float durata, Runnable dopo)
    {
        transizione = true;
        nero.clearActions();
        nero.toFront();
        nero.addAction(Actions.sequence(Actions.fadeIn(durata), Actions.run(dopo)));
    }

    // =====================================================================
    // Pausa e input
    // =====================================================================

    private boolean puoMettereInPausa()
    {
        return !gameOver && !transizione && !cutscene && !esploratore.isDead();
    }

    /** Se la finestra viene ridotta a icona, il gioco va in pausa da solo. */
    @Override
    public void pause()
    {
        if (!pausa && esploratore != null && puoMettereInPausa())
            alternaPausa();
    }

    private void alternaPausa()
    {
        pausa = !pausa;
        mostraPausa(pausa);
        if (pausa) Risorse.pausaMusica();
        else Risorse.musica(isInPassato);
    }

    @Override
    public boolean keyDown(int keyCode)
    {
        if (keyCode == Input.Keys.ESCAPE || keyCode == Input.Keys.P) {
            if (puoMettereInPausa()) alternaPausa();
            return true;
        }
        if (transizione) return false;
        if (pausa) return pausaBottoni.gestisciTasto(keyCode);
        if (gameOver) {
            if (keyCode == Input.Keys.R) { riprova(); return true; }
            return goBottoni.gestisciTasto(keyCode);
        }
        if (cutscene) {
            if (cutsceneChiudibile && (keyCode == Input.Keys.E || keyCode == Input.Keys.ENTER || keyCode == Input.Keys.SPACE))
                chiudiCutscene();
            return true;
        }
        if (esploratore.isDead()) return false;

        if (keyCode == Input.Keys.SPACE || keyCode == Input.Keys.W || keyCode == Input.Keys.UP)
            esploratore.premiSalto();
        if (keyCode == Input.Keys.E)
            interagisci();
        if (keyCode == Input.Keys.T)
            viaggiaNelTempo();
        return false;
    }

    @Override
    public boolean keyUp(int keyCode)
    {
        if (keyCode == Input.Keys.SPACE || keyCode == Input.Keys.W || keyCode == Input.Keys.UP)
            esploratore.rilasciaSalto();
        return false;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        mappaPresente.dispose();
        if (mappaPassato != null) mappaPassato.dispose();
    }
}
