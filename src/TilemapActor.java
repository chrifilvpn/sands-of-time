import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import java.util.ArrayList;

/**
 *  Carica una mappa creata con Tiled (*.tmx) e la disegna come Actor.
 *
 *  Le proprietà della mappa (Mappa > Proprietà mappa in Tiled) si leggono con
 *  getProprietaTesto / getProprietaNumero: così ogni livello può avere le sue impostazioni
 *  senza scrivere "if (livello == ...)" nel codice.
 */
public class TilemapActor extends Actor
{
    private TiledMap tiledMap;
    private OrthographicCamera tiledCamera;
    private OrthoCachedTiledMapRenderer tiledMapRenderer;
    private boolean distrutta = false;

    public TilemapActor(String filename, Stage theStage)
    {
        TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
        params.textureMinFilter = Texture.TextureFilter.Nearest;
        params.textureMagFilter = Texture.TextureFilter.Nearest;
        tiledMap = new TmxMapLoader().load(filename, params);

        tiledMapRenderer = new OrthoCachedTiledMapRenderer(tiledMap);
        tiledMapRenderer.setBlending(true);
        tiledCamera = new OrthographicCamera();
        tiledCamera.setToOrtho(false, BaseScreen.LARGHEZZA, BaseScreen.ALTEZZA);
        tiledCamera.update();
        theStage.addActor(this);
        BaseActor.setWorldBounds(getMapWidth(), getMapHeight());
    }

    // ----------------------------------------------
    // Oggetti della mappa
    // ----------------------------------------------

    /** Tutti gli oggetti (di qualsiasi forma) che hanno il nome indicato. */
    public ArrayList<MapObject> getObjectList(String nome)
    {
        ArrayList<MapObject> list = new ArrayList<MapObject>();
        for (MapLayer layer : tiledMap.getLayers())
            for (MapObject obj : layer.getObjects())
                if (nome.equals(obj.getName()))
                    list.add(obj);
        return list;
    }

    /** Solo gli oggetti rettangolari che hanno il nome indicato. */
    public ArrayList<MapObject> getRectangleList(String nome)
    {
        ArrayList<MapObject> list = new ArrayList<MapObject>();
        for (MapObject obj : getObjectList(nome))
            if (obj instanceof RectangleMapObject)
                list.add(obj);
        return list;
    }

    // ----------------------------------------------
    // Proprietà della mappa
    // ----------------------------------------------

    public String getProprietaTesto(String nome, String predefinito)
    {
        Object v = tiledMap.getProperties().get(nome);
        return (v == null) ? predefinito : v.toString();
    }

    public float getProprietaNumero(String nome, float predefinito)
    {
        Object v = tiledMap.getProperties().get(nome);
        if (v == null) return predefinito;
        if (v instanceof Number) return ((Number) v).floatValue();
        try {
            return Float.parseFloat(v.toString().trim());
        } catch (NumberFormatException e) {
            return predefinito;
        }
    }

    // ----------------------------------------------
    // Disegno
    // ----------------------------------------------

    @Override
    public void draw(Batch batch, float parentAlpha)
    {
        if (distrutta) return;
        Camera mainCamera = getStage().getCamera();
        tiledCamera.position.x = Math.round(mainCamera.position.x);
        tiledCamera.position.y = Math.round(mainCamera.position.y);
        tiledCamera.update();
        tiledMapRenderer.setView(tiledCamera);
        batch.end();
        tiledMapRenderer.render();
        batch.begin();
    }

    public int getMapWidth()
    {
        MapProperties p = tiledMap.getProperties();
        return (Integer) p.get("tilewidth") * (Integer) p.get("width");
    }

    public int getMapHeight()
    {
        MapProperties p = tiledMap.getProperties();
        return (Integer) p.get("tileheight") * (Integer) p.get("height");
    }

    public TiledMap getTiledMap()
    {
        return tiledMap;
    }

    /** Libera le texture della mappa (prima non veniva mai fatto). */
    public void dispose()
    {
        if (distrutta) return;
        distrutta = true;
        tiledMapRenderer.dispose();
        tiledMap.dispose();
    }
}
