import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;

/**
 * Tutte le risorse condivise del gioco, caricate UNA volta all'avvio e liberate alla chiusura.
 * Prima font, texture e musica venivano ricreati a ogni livello (e a ogni "Riprova") senza mai essere liberati.
 */
public class Risorse
{
    // ---------------- font ----------------
    public static BitmapFont fontPiccolo;   // 10
    public static BitmapFont fontNormale;   // 12
    public static BitmapFont fontMedio;     // 16
    public static BitmapFont fontGrande;    // 22
    public static BitmapFont fontTitolo;    // 36
    public static BitmapFont fontEnorme;    // 48

    // ---------------- grafica ----------------
    /** Texture bianca 1x1: colorata con setColor diventa qualsiasi overlay o rettangolo. */
    public static Texture bianco;
    public static Texture vignetta;
    public static Texture artefattoSpento, artefattoAcceso, ingranaggio, sfondoMenu;
    public static NinePatchDrawable bottoneNormale, bottoneSelezionato, bottoneDisabilitato, pannello;

    // ---------------- animazioni del personaggio (condivise) ----------------
    public static Animation<TextureRegion> animIdle, animCorsa, animSalto, animMorte;

    // ---------------- audio ----------------
    public static Sound sSalto, sMorte, sTempo, sBloccato, sArtefatto, sPorta, sClick, sSeleziona, sVittoria;
    public static Music musicaPresente, musicaPassato;
    public static final float VOLUME_MUSICA = 0.45f;
    public static final float VOLUME_EFFETTI = 0.6f;
    private static boolean muto = false;

    /** Numero di livelli presenti nella cartella assets (mappaN_presente.tmx). */
    public static int numeroLivelli;

    private static final ArrayList<Disposable> daLiberare = new ArrayList<Disposable>();

    public static void carica()
    {
        // ---- font pixel ----
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("assets/PressStart2P-Regular.ttf"));
        fontPiccolo = creaFont(gen, 10, 1);
        fontNormale = creaFont(gen, 12, 1);
        fontMedio   = creaFont(gen, 16, 2);
        fontGrande  = creaFont(gen, 22, 2);
        fontTitolo  = creaFont(gen, 36, 3);
        fontEnorme  = creaFont(gen, 48, 3);
        gen.dispose();

        // ---- texture generate ----
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        bianco = registra(new Texture(p));
        p.dispose();

        vignetta = registra(creaVignetta(200, 120));
        vignetta.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        bottoneNormale      = creaRiquadro(new Color(0.08f, 0.06f, 0.03f, 0.88f), new Color(0.85f, 0.65f, 0.25f, 1f));
        bottoneSelezionato  = creaRiquadro(new Color(0.85f, 0.62f, 0.20f, 0.95f), new Color(1f, 0.92f, 0.6f, 1f));
        bottoneDisabilitato = creaRiquadro(new Color(0.08f, 0.08f, 0.08f, 0.75f), new Color(0.35f, 0.35f, 0.35f, 1f));
        pannello            = creaRiquadro(new Color(0.06f, 0.04f, 0.02f, 0.92f), new Color(0.85f, 0.65f, 0.25f, 1f));

        // ---- immagini (tramite la cache di BaseActor) ----
        artefattoSpento = BaseActor.getTexture("assets/artefatto_s.png");
        artefattoAcceso = BaseActor.getTexture("assets/artefatto_a.png");
        ingranaggio     = BaseActor.getTexture("assets/ingranaggio.png");
        sfondoMenu      = BaseActor.getTexture("assets/menu.png");

        // ---- animazioni ----
        animIdle  = animazione("assets/Idle/Idle", 1, 4, 0.5f, true);
        animCorsa = animazione("assets/Run/run", 1, 6, 0.09f, true);
        animSalto = animazione("assets/Jump/jump", 1, 8, 0.1f, true);
        animMorte = animazione("assets/Death/death", 1, 7, 0.12f, false);

        // ---- audio ----
        sSalto     = suono("salto");
        sMorte     = suono("morte");
        sTempo     = suono("tempo");
        sBloccato  = suono("bloccato");
        sArtefatto = suono("artefatto");
        sPorta     = suono("porta");
        sClick     = suono("click");
        sSeleziona = suono("seleziona");
        sVittoria  = suono("vittoria");

        musicaPresente = registra(Gdx.audio.newMusic(Gdx.files.internal("assets/musica_presente.mp3")));
        musicaPresente.setLooping(true);
        musicaPassato = registra(Gdx.audio.newMusic(Gdx.files.internal("assets/musica_passato.mp3")));
        musicaPassato.setLooping(true);

        numeroLivelli = 0;
        while (Gdx.files.internal("assets/mappa" + (numeroLivelli + 1) + "_presente.tmx").exists())
            numeroLivelli++;
    }

    public static void dispose()
    {
        for (Disposable d : daLiberare)
            d.dispose();
        daLiberare.clear();
    }

    // =====================================================================
    // Font ed etichette
    // =====================================================================

    private static BitmapFont creaFont(FreeTypeFontGenerator gen, int dimensione, int bordo)
    {
        FreeTypeFontGenerator.FreeTypeFontParameter par = new FreeTypeFontGenerator.FreeTypeFontParameter();
        par.size = dimensione;
        par.color = Color.WHITE;
        par.borderWidth = bordo;
        par.borderColor = new Color(0, 0, 0, 0.85f);
        par.shadowOffsetX = Math.max(1, bordo);
        par.shadowOffsetY = Math.max(1, bordo);
        par.shadowColor = new Color(0, 0, 0, 0.5f);
        return registra(gen.generateFont(par));
    }

    /** Crea un'etichetta con il font e il colore indicati. */
    public static Label etichetta(String testo, BitmapFont font, Color colore)
    {
        Label.LabelStyle stile = new Label.LabelStyle(font, colore);
        Label l = new Label(testo, stile);
        l.pack();
        return l;
    }

    // =====================================================================
    // Grafica generata via codice
    // =====================================================================

    /** Riquadro con bordo (per bottoni e pannelli), ridimensionabile senza deformare il bordo. */
    private static NinePatchDrawable creaRiquadro(Color sfondo, Color bordo)
    {
        Pixmap p = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
        p.setColor(bordo);
        p.fill();
        p.setColor(sfondo);
        p.fillRectangle(2, 2, 8, 8);
        Texture t = registra(new Texture(p));
        p.dispose();
        return new NinePatchDrawable(new NinePatch(t, 3, 3, 3, 3));
    }

    /**
     * Vignetta azzurra per il "passato". Generata piccola e poi allungata:
     * prima veniva calcolata pixel per pixel (384.000 pixel) a ogni livello.
     */
    private static Texture creaVignetta(int w, int h)
    {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        float cx = w / 2f, cy = h / 2f;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float dx = (x - cx) / cx;
                float dy = (y - cy) / cy;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / 1.4142f;
                float alpha = Math.min(d * d * 0.75f, 0.6f);
                pixmap.setColor(0.3f, 0.7f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }

    private static Animation<TextureRegion> animazione(String prefisso, int primo, int ultimo, float durata, boolean loop)
    {
        Array<TextureRegion> frame = new Array<TextureRegion>();
        for (int i = primo; i <= ultimo; i++)
            frame.add(new TextureRegion(BaseActor.getTexture(prefisso + i + ".png")));
        Animation<TextureRegion> a = new Animation<TextureRegion>(durata, frame);
        a.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return a;
    }

    // =====================================================================
    // Audio
    // =====================================================================

    private static Sound suono(String nome)
    {
        FileHandle f = Gdx.files.internal("assets/suoni/" + nome + ".wav");
        if (!f.exists()) return null;
        return registra(Gdx.audio.newSound(f));
    }

    /** Riproduce un effetto sonoro (se esiste e l'audio non è disattivato). */
    public static void suona(Sound s)
    {
        if (s != null && !muto)
            s.play(VOLUME_EFFETTI);
    }

    /**
     * Fa suonare la musica dell'epoca indicata, mettendo in pausa l'altra.
     * La musica NON riparte da capo quando si cambia livello: continua da dove era.
     */
    public static void musica(boolean passato)
    {
        Music attiva = passato ? musicaPassato : musicaPresente;
        Music altra  = passato ? musicaPresente : musicaPassato;
        if (altra.isPlaying()) altra.pause();
        attiva.setVolume(muto ? 0 : VOLUME_MUSICA);
        if (!attiva.isPlaying()) attiva.play();
    }

    public static void pausaMusica()
    {
        if (musicaPresente.isPlaying()) musicaPresente.pause();
        if (musicaPassato.isPlaying()) musicaPassato.pause();
    }

    public static void fermaMusica()
    {
        musicaPresente.stop();
        musicaPassato.stop();
    }

    /** Tasto M: toglie / rimette l'audio. */
    public static void alternaMuto()
    {
        muto = !muto;
        float v = muto ? 0 : VOLUME_MUSICA;
        musicaPresente.setVolume(v);
        musicaPassato.setVolume(v);
    }

    public static boolean isMuto()
    {
        return muto;
    }

    private static <T extends Disposable> T registra(T risorsa)
    {
        daLiberare.add(risorsa);
        return risorsa;
    }
}
