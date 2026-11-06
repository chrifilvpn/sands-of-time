import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import java.util.ArrayList;

/**
 * Piccolo sistema di particelle (quadratini colorati) usato per gli effetti grafici:
 * polvere nell'aria, sbuffi di sabbia all'atterraggio, scintille del viaggio nel tempo,
 * luccichio delle uscite e dell'artefatto.
 *
 * Se "ambientale" è true, le particelle riempiono lo schermo e ricompaiono quando escono
 * (granelli di sabbia che fluttuano).
 */
public class Particelle extends Actor
{
    private static class P {
        float x, y, vx, vy, vita, vitaMax, dim, gravita;
        float r, g, b, a;
        boolean amb;
    }

    private final ArrayList<P> lista = new ArrayList<P>();
    private float larghezzaAmb, altezzaAmb;
    private final Color coloreAmb = new Color();
    private float tempo = 0;

    public Particelle(Stage stage)
    {
        setTouchable(Touchable.disabled);
        stage.addActor(this);
    }

    /** Attiva la polvere ambientale su un'area (in coordinate dello stage). */
    public void avviaAmbiente(int quante, float larghezza, float altezza, Color colore)
    {
        larghezzaAmb = larghezza;
        altezzaAmb = altezza;
        coloreAmb.set(colore);
        for (int i = 0; i < quante; i++) {
            P p = nuovaAmbientale();
            p.vita = MathUtils.random(p.vitaMax);
            lista.add(p);
        }
    }

    /** Cambia il colore della polvere ambientale (es. presente / passato). */
    public void setColoreAmbiente(Color c)
    {
        coloreAmb.set(c);
        for (P p : lista) {
            if (p.amb) { p.r = c.r; p.g = c.g; p.b = c.b; }
        }
    }

    private P nuovaAmbientale()
    {
        P p = new P();
        p.amb = true;
        p.x = MathUtils.random(larghezzaAmb);
        p.y = MathUtils.random(altezzaAmb);
        p.vx = MathUtils.random(6f, 22f);
        p.vy = MathUtils.random(-6f, 6f);
        p.vitaMax = MathUtils.random(4f, 9f);
        p.vita = 0;
        p.dim = MathUtils.random() < 0.8f ? 2 : 3;
        p.r = coloreAmb.r; p.g = coloreAmb.g; p.b = coloreAmb.b;
        p.a = MathUtils.random(0.25f, 0.6f);
        return p;
    }

    /** Emette un gruppo di particelle in un punto. */
    public void esplosione(float x, float y, int quante, Color colore, float velocita, float vitaMax, float gravita)
    {
        for (int i = 0; i < quante; i++) {
            P p = new P();
            float ang = MathUtils.random(360f);
            float v = MathUtils.random(velocita * 0.3f, velocita);
            p.x = x; p.y = y;
            p.vx = MathUtils.cosDeg(ang) * v;
            p.vy = MathUtils.sinDeg(ang) * v;
            p.vitaMax = MathUtils.random(vitaMax * 0.5f, vitaMax);
            p.dim = MathUtils.random(2, 4);
            p.gravita = gravita;
            p.r = colore.r; p.g = colore.g; p.b = colore.b; p.a = colore.a;
            lista.add(p);
        }
    }

    /** Sbuffo di sabbia verso i lati (atterraggio). */
    public void sbuffo(float x, float y, Color colore)
    {
        for (int i = 0; i < 8; i++) {
            P p = new P();
            p.x = x + MathUtils.random(-6f, 6f); p.y = y + MathUtils.random(0f, 3f);
            p.vx = MathUtils.random(25f, 70f) * (i % 2 == 0 ? 1 : -1);
            p.vy = MathUtils.random(8f, 30f);
            p.vitaMax = MathUtils.random(0.25f, 0.5f);
            p.dim = MathUtils.random(2, 3);
            p.gravita = 120;
            p.r = colore.r; p.g = colore.g; p.b = colore.b; p.a = colore.a;
            lista.add(p);
        }
    }

    /** Scintilla singola che sale lentamente da un rettangolo (uscite, artefatto). */
    public void scintilla(float x, float y, float w, float h, Color colore)
    {
        P p = new P();
        p.x = x + MathUtils.random(w);
        p.y = y + MathUtils.random(h);
        p.vx = MathUtils.random(-5f, 5f);
        p.vy = MathUtils.random(10f, 28f);
        p.vitaMax = MathUtils.random(0.6f, 1.3f);
        p.dim = MathUtils.random() < 0.7f ? 2 : 3;
        p.gravita = 0;
        p.r = colore.r; p.g = colore.g; p.b = colore.b; p.a = colore.a;
        lista.add(p);
    }

    @Override
    public void act(float dt)
    {
        super.act(dt);
        tempo += dt;
        for (int i = lista.size() - 1; i >= 0; i--) {
            P p = lista.get(i);
            p.vita += dt;
            p.vy -= p.gravita * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (p.vita >= p.vitaMax) {
                if (p.amb)
                    lista.set(i, nuovaAmbientale());
                else
                    lista.remove(i);
            } else if (p.amb) {
                // i granelli ambientali ondeggiano e rientrano dai bordi
                p.vy += MathUtils.sin(tempo * 1.3f + p.x * 0.05f) * 4f * dt;
                if (p.x > larghezzaAmb + 4) p.x = -4;
                if (p.y < -4) p.y = altezzaAmb + 4;
                if (p.y > altezzaAmb + 4) p.y = -4;
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha)
    {
        for (P p : lista) {
            float t = p.vita / p.vitaMax;
            // dissolvenza in entrata e in uscita
            float alpha = p.a * Math.min(1f, Math.min(t * 5f, (1f - t) * 3f));
            if (alpha <= 0) continue;
            batch.setColor(p.r, p.g, p.b, alpha * parentAlpha * getColor().a);
            batch.draw(Risorse.bianco, Math.round(p.x), Math.round(p.y), p.dim, p.dim);
        }
        batch.setColor(1, 1, 1, 1);
    }
}
