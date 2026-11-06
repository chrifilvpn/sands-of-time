import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import java.util.List;

/**
 * Il protagonista: l'esploratore.
 *
 * Migliorie rispetto alla versione originale:
 * - controlli con FRECCE oppure A/D, salto con SPAZIO, W o FRECCIA SU;
 * - salto variabile (se lasci il tasto presto salti più basso);
 * - "coyote time": puoi saltare per un attimo anche dopo essere uscito da una piattaforma;
 * - "jump buffer": se premi salto poco prima di atterrare, il salto parte appena tocchi terra;
 * - la hitbox combacia con il disegno (prima i piedi "galleggiavano" sopra il pavimento);
 * - le animazioni sono condivise (classe Risorse) invece di essere ricaricate a ogni livello.
 */
public class Esploratore extends BaseActor
{
    // fisica (stessi valori della versione originale)
    private static final float VEL_MAX_X = 150;
    private static final float GRAVITA = 700;
    private static final float VEL_SALTO = 450;
    private static final float VEL_MAX_Y = 400;

    private static final float TEMPO_COYOTE = 0.10f;
    private static final float TEMPO_BUFFER = 0.12f;
    private static final float TAGLIO_SALTO = 0.45f;  // velocità mantenuta se si lascia il tasto

    // posizione del personaggio dentro l'immagine (in proporzione): servono per la hitbox
    private static final float PIEDI = 0.13f;
    private static final float TESTA = 0.86f;
    private static final float LATO  = 0.31f;

    private final Animation<TextureRegion> idle, walk, jump, death;
    private final BaseActor sensoreSotto;
    private final float dimensione;

    private boolean versoSinistra = false;
    private boolean morto = false;
    private boolean aTerra = false;
    private float timerMorte = 0f;
    private float timerCoyote = 0f;
    private float timerBuffer = 0f;

    /**
     * @param dimensione lato (in pixel) dell'immagine del personaggio: si imposta nella mappa
     *                   con la proprietà "dimensionePersonaggio".
     */
    public Esploratore(float x, float y, Stage s, float dimensione)
    {
        super(x, y, s);
        this.dimensione = dimensione;

        idle  = Risorse.animIdle;
        walk  = Risorse.animCorsa;
        jump  = Risorse.animSalto;
        death = Risorse.animMorte;
        setAnimation(idle);

        float w = getWidth(), h = getHeight();
        float[] vertici = {
                w * LATO, h * PIEDI,
                w * (1 - LATO), h * PIEDI,
                w * (1 - LATO), h * TESTA,
                w * LATO, h * TESTA
        };
        boundaryPolygon = new Polygon(vertici);

        sensoreSotto = new BaseActor(0, 0, s);
        sensoreSotto.setSize(w * (1 - 2 * LATO) * 0.9f, 4);
        sensoreSotto.setBoundaryRectangle();
        sensoreSotto.setVisible(false);
        aggiornaSensore();
    }

    /** Sposta il personaggio in modo che i PIEDI siano al centro del punto indicato. */
    public void posizionaPiedi(float centroX, float y)
    {
        setPosition(centroX - getWidth() / 2f, y - getHeight() * PIEDI);
        aggiornaSensore();
    }

    // =====================================================================
    // Input e movimento (chiamati da LevelScreen.update)
    // =====================================================================

    /** Legge la tastiera per il movimento orizzontale e aggiorna i timer di salto. */
    public void leggiInput(float dt)
    {
        if (morto) return;
        boolean sx = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean dx = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);

        if (sx && !dx)      velocityVec.x = -VEL_MAX_X;
        else if (dx && !sx) velocityVec.x = VEL_MAX_X;
        else                velocityVec.x = 0;

        if (velocityVec.x > 0) versoSinistra = false;
        if (velocityVec.x < 0) versoSinistra = true;

        timerBuffer -= dt;
        timerCoyote -= dt;
    }

    /** Il giocatore ha premuto il tasto di salto. */
    public void premiSalto()
    {
        timerBuffer = TEMPO_BUFFER;
    }

    /** Il giocatore ha lasciato il tasto di salto: salto più basso. */
    public void rilasciaSalto()
    {
        if (velocityVec.y > 0)
            velocityVec.y *= TAGLIO_SALTO;
    }

    /**
     * Dopo le collisioni: decide se saltare e quale animazione mostrare.
     * @return true se è appena partito un salto (per il suono)
     */
    public boolean aggiornaStato(List<Oggetto> solidi)
    {
        if (morto) return false;
        aggiornaSensore();
        aTerra = velocityVec.y <= 0 && toccaSolido(solidi);
        if (aTerra) timerCoyote = TEMPO_COYOTE;

        boolean salto = false;
        if (timerBuffer > 0 && timerCoyote > 0) {
            velocityVec.y = VEL_SALTO;
            timerBuffer = 0;
            timerCoyote = 0;
            aTerra = false;
            salto = true;
        }

        if (aTerra) setAnimation(velocityVec.x == 0 ? idle : walk);
        else        setAnimation(jump);
        return salto;
    }

    private boolean toccaSolido(List<Oggetto> solidi)
    {
        for (Oggetto o : solidi)
            if (o.isEnable() && sensoreSotto.overlaps(o))
                return true;
        return false;
    }

    public void applyGravity(float dt)
    {
        if (morto) return;
        velocityVec.y -= GRAVITA * dt;
        velocityVec.y = MathUtils.clamp(velocityVec.y, -VEL_MAX_Y, VEL_MAX_Y);
    }

    public void moveX(float dt)
    {
        if (morto) return;
        moveBy(velocityVec.x * dt, 0);
    }

    public void moveY(float dt)
    {
        if (morto) return;
        moveBy(0, velocityVec.y * dt);
    }

    public void aggiornaSensore()
    {
        sensoreSotto.setPosition(getX() + getWidth() * 0.5f - sensoreSotto.getWidth() / 2f,
                                 getY() + getHeight() * PIEDI - 3);
    }

    /** Tiene il personaggio dentro i confini della mappa (in base alla hitbox, non all'immagine). */
    public void restaNelMondo()
    {
        Rectangle m = getWorldBounds();
        float sx = getX() + getWidth() * LATO;
        float dx = getX() + getWidth() * (1 - LATO);
        float giu = getY() + getHeight() * PIEDI;
        float su  = getY() + getHeight() * TESTA;
        if (sx < 0) moveBy(-sx, 0);
        if (dx > m.width) moveBy(m.width - dx, 0);
        if (giu < 0) { moveBy(0, -giu); if (velocityVec.y < 0) velocityVec.y = 0; }
        if (su > m.height) { moveBy(0, m.height - su); if (velocityVec.y > 0) velocityVec.y = 0; }
    }

    /** Rettangolo della hitbox nel mondo, rimpicciolito di "margine" pixel per lato. */
    public Rectangle getHitbox(float margine)
    {
        return new Rectangle(
                getX() + getWidth() * LATO + margine,
                getY() + getHeight() * PIEDI + margine,
                getWidth() * (1 - 2 * LATO) - 2 * margine,
                getHeight() * (TESTA - PIEDI) - 2 * margine);
    }

    public boolean isATerra() { return aTerra; }

    /** Coordinata Y dei piedi. */
    public float getPiediY() { return getY() + getHeight() * PIEDI; }

    /** Centro della hitbox (per la telecamera e le etichette). */
    public float getCentroX() { return getX() + getWidth() / 2f; }
    public float getCentroY() { return getY() + getHeight() * (PIEDI + TESTA) / 2f; }

    // =====================================================================
    // Morte
    // =====================================================================

    public void morte()
    {
        if (morto) return;
        morto = true;
        timerMorte = 0f;
        velocityVec.set(0, 0);
        setAnimation(death);
    }

    public boolean isDeathFinished()
    {
        return morto && timerMorte >= death.getAnimationDuration() + 0.35f;
    }

    public boolean isDead()
    {
        return morto;
    }

    // =====================================================================
    // Aggiornamento e disegno
    // =====================================================================

    @Override
    public void act(float dt)
    {
        super.act(dt);
        if (morto) timerMorte += dt;
    }

    @Override
    public void setAnimation(Animation<TextureRegion> anim)
    {
        super.setAnimation(anim);
        // tutte le immagini sono quadrate: la dimensione del personaggio è fissa
        setSize(dimensione, dimensione);
        setOrigin(dimensione / 2, dimensione / 2);
    }

    @Override
    public void draw(Batch batch, float parentAlpha)
    {
        if (!isVisible() || getAnimation() == null) return;
        com.badlogic.gdx.graphics.Color c = getColor();
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

        float tempo = morto ? timerMorte : getElapsedTime();
        TextureRegion frame = getAnimation().getKeyFrame(tempo);

        float drawX = getX();
        float drawW = getWidth();
        if (versoSinistra) {
            drawX = getX() + getWidth();
            drawW = -getWidth();
        }
        batch.draw(frame, drawX, getY(), getOriginX(), getOriginY(),
                drawW, getHeight(), 1, 1, getRotation());
        batch.setColor(1, 1, 1, 1);
    }
}
