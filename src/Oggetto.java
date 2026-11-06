import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;

/**
 * Oggetto invisibile letto dalla mappa Tiled: muro/pavimento, trappola, uscita o oggetto da ispezionare.
 */
public class Oggetto extends BaseActor
{
    public enum Tipo { SOLIDO, TRAPPOLA, USCITA, INTERAZIONE }

    private final Tipo tipo;
    private boolean enable;

    public Oggetto(float x, float y, float width, float height, Tipo tipo, Stage stage)
    {
        super(x, y, stage);
        this.tipo = tipo;
        setSize(width, height);
        setBoundaryRectangle();
        enable = true;
    }

    /**
     * Crea un Oggetto a partire da un oggetto della mappa.
     * Funziona sia con i rettangoli sia con i poligoni (dei poligoni si usa il rettangolo che li contiene).
     * Prima questo codice era copiato 6 volte in LevelScreen.
     */
    public static Oggetto daMappa(MapObject obj, Tipo tipo, Stage stage)
    {
        float x, y, w, h;
        MapProperties props = obj.getProperties();
        if (obj instanceof PolygonMapObject) {
            Polygon poly = ((PolygonMapObject) obj).getPolygon();
            poly.setPosition(numero(props.get("x")), numero(props.get("y")));
            Rectangle box = poly.getBoundingRectangle();
            x = box.x; y = box.y; w = box.width; h = box.height;
        } else {
            x = numero(props.get("x"));
            y = numero(props.get("y"));
            w = numero(props.get("width"));
            h = numero(props.get("height"));
        }
        return new Oggetto(x, y, w, h, tipo, stage);
    }

    private static float numero(Object v)
    {
        return (v instanceof Number) ? ((Number) v).floatValue() : 0f;
    }

    public Tipo getTipo()
    {
        return tipo;
    }

    public void setEnable(boolean enable)
    {
        this.enable = enable;
    }

    public boolean isEnable()
    {
        return enable;
    }

    /** Rettangolo dell'oggetto nel mondo. */
    public Rectangle getRettangolo()
    {
        return new Rectangle(getX(), getY(), getWidth(), getHeight());
    }
}
