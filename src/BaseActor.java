
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array; 
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.Batch;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector;

import java.util.ArrayList;
import java.util.HashMap;
import com.badlogic.gdx.math.Rectangle;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 *
 */
public class BaseActor extends Actor
{
    private Animation<TextureRegion> animation;
    private float elapsedTime;
    //utilizzata per consentire di mettere in pausa il gioco
    private boolean animationPaused;
    /*
     * La classe Vector2 viene utilizzata per stabilire la velocità di movimento di un personaggio.
     * Semplifica le formule matematiche utilizzate per calcolare lo spostamento e la direzione
     * di una figura in un arco di tempo che in GDX viene calcolato in pixel/secondo
     */
    protected Vector2 velocityVec;
    protected Vector2 accelerationVec;
    private float acceleration;
    private float maxSpeed;
    private float deceleration;

    //nelle versioni precedenti le collisioni venivano gestite con rettangoli, ma non c'era precisione
    // ora verranno gestite con i poligoni, ci consente di rendere più realistica la collisione
    protected Polygon boundaryPolygon;

    // dimensioni del mondo di gioco, condivise da tutti gli attori
    private static Rectangle worldBounds = new Rectangle(0, 0, 800, 480);

    // cache delle texture: ogni immagine viene caricata una sola volta per tutto il gioco
    // (prima ogni livello ricaricava le stesse immagini senza mai liberarle)
    private static final HashMap<String, Texture> cacheTexture = new HashMap<String, Texture>();

    /** Restituisce la texture del file indicato, caricandola solo la prima volta. */
    public static Texture getTexture(String fileName)
    {
        Texture t = cacheTexture.get(fileName);
        if (t == null) {
            t = new Texture( Gdx.files.internal(fileName) );
            t.setFilter( TextureFilter.Linear, TextureFilter.Linear );
            cacheTexture.put(fileName, t);
        }
        return t;
    }

    /** Libera tutte le texture in cache (da chiamare alla chiusura del gioco). */
    public static void disposeTextures()
    {
        for (Texture t : cacheTexture.values())
            t.dispose();
        cacheTexture.clear();
    }
    
    public BaseActor(float x, float y, Stage s)
    {
        // call constructor from Actor class
        super();

        // perform additional initialization tasks
        setPosition(x,y);
        s.addActor(this);

        // inizializzazione dei parametri
        animation = null;
        elapsedTime = 0;
        animationPaused = false;

        // initialize physics data
        velocityVec = new Vector2(0,0);
        accelerationVec = new Vector2(0,0);
        acceleration = 0;
        maxSpeed = 1000;
        deceleration = 0;

        boundaryPolygon = null;
    }

    /**
     * Il metodo setPosition di un actor imposta l'angolo in basso a sinistra
     * Questo metodo lo centra perfettamente per impostare l'esatta posizione dell'oggetto
     */
    public void centerAtPosition(float x, float y)
    {
        setPosition( x - getWidth()/2 , y - getHeight()/2 );
    }

    /** 
     *  Centra un oggetto rispetto ad un altro
     */
    public void centerAtActor(BaseActor other)
    {
        centerAtPosition( other.getX() + other.getWidth()/2 , other.getY() + other.getHeight()/2 );
    }

    /**
     * Metodo che imposta l'animazione del personaggio, calcolando anche il centro sulla base delle dimansioni della prima immagine
    */
    public void setAnimation(Animation<TextureRegion> anim)
    {
        // quando cambia animazione riparte dal primo fotogramma
        if (anim != animation)
            elapsedTime = 0;
        animation = anim;
        TextureRegion tr = animation.getKeyFrame(0);
        float w = tr.getRegionWidth();
        float h = tr.getRegionHeight();
        setSize( w, h );
        setOrigin( w/2, h/2 );

        if (boundaryPolygon == null)
            setBoundaryRectangle();
    }

    /**
     Metodo utilizzato per passare le immagini singole in un unico array
     PlayMode serve per stabilire come eseguire le animazioni
     normal: le immagini sono visualizzate in successione con il movimento del personaggio
     loop: la sequenza di immagini sarà visualizzata continuamente
     */
    public Animation<TextureRegion> loadAnimationFromFiles(String[] fileNames, float frameDuration, boolean loop)
    { 
        int fileCount = fileNames.length;
        Array<TextureRegion> textureArray = new Array<TextureRegion>();

        for (int n = 0; n < fileCount; n++)
        {   
            textureArray.add( new TextureRegion( getTexture(fileNames[n]) ) );
        }

        Animation<TextureRegion> anim = new Animation<TextureRegion>(frameDuration, textureArray);

        if (loop)
            anim.setPlayMode(PlayMode.LOOP);
        else
            anim.setPlayMode(PlayMode.NORMAL);

        if (animation == null)
            setAnimation(anim);

        return anim;
    }

    /**
     * Metodo che carica le immagini da un unico file, l'importante è che le immagini siano dello stesso numero su ogni riga.
     * Verrà utilizzato il metodo split per prelevarle una per volta definendo il numero di righe e colonne
     */
    public Animation<TextureRegion> loadAnimationFromSheet(String fileName, int rows, int cols, float frameDuration, boolean loop)
    { 
        Texture texture = getTexture(fileName);
        int frameWidth = texture.getWidth() / cols;
        int frameHeight = texture.getHeight() / rows;

        TextureRegion[][] temp = TextureRegion.split(texture, frameWidth, frameHeight);

        Array<TextureRegion> textureArray = new Array<TextureRegion>();

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                textureArray.add( temp[r][c] );

        Animation<TextureRegion> anim = new Animation<TextureRegion>(frameDuration, textureArray);

        if (loop)
            anim.setPlayMode(PlayMode.LOOP);
        else
            anim.setPlayMode(PlayMode.NORMAL);

        if (animation == null)
            setAnimation(anim);

        return anim;
    }

    /**
     * Questo metodo viene usato quando il personaggio ha un'unica immagine
     */
    public Animation<TextureRegion> loadTexture(String fileName)
    {
        String[] fileNames = new String[1];
        fileNames[0] = fileName;
        return loadAnimationFromFiles(fileNames, 1, true);
    }

    /**
     * Metodo usato per mettere in pausa l'animazione del personaggio
     */
    public void setAnimationPaused(boolean pause)
    {
        animationPaused = pause;
    }

    /**
     *  Questo metodo serve per stabilire se l'animazione è terminata (solo quando non è in loop)
     *  SI verifica quando il tempo trascorso è superiore al tempo necessario per visualizzare tutte le immagini
     *
     */
    public boolean isAnimationFinished()
    {

        return animation.isAnimationFinished(elapsedTime);
    }


    public void setOpacity(float opacity)
    {
        this.getColor().a = opacity;
    }

    /**
     *  Imposta l'accelerazione del personaggio.
     *  @param acc Accelerazione impostata in (pixels/secondo) per ogni secondo.
     */
    public void setAcceleration(float acc)
    {
        acceleration = acc;
    }

    /**
     *  La decelerazione serve quando il personaggio non è in movimento
     *  @param dec decelerazione impostata in (pixels/secondo) per ogni secondo.
     */
    public void setDeceleration(float dec)
    {
        deceleration = dec;
    }

    /**
     *  Velocità massima del personaggio
     *  @param ms Massima velocità in pixels/secondo.
     */
    public void setMaxSpeed(float ms)
    {
        maxSpeed = ms;
    }

    /**
     *  Velocità del movimento del personaggio.
     *  Se la velocità è 0 allora  la direzione non è definita e quindi impostata a 0 gradi.
     *  @param speed  (pixels/secondo)
     */
    public void setSpeed(float speed)
    {
        // if length is zero, then assume motion angle is zero degrees
        if (velocityVec.len() == 0)
            velocityVec.set(speed, 0);
        else
            velocityVec.setLength(speed);
    }

    /**
     *  Calculates the speed of movement (in pixels/second).
     *  @return speed of movement (pixels/second)
     */
    public float getSpeed()
    {
        return velocityVec.len();
    }

    /**
     *  Determina se l'oggetto è in movimento
     *  @return false se velocità è 0, altrimenti true
     */
    public boolean isMoving()
    {
        return (getSpeed() > 0);
    }

    /**
     Imposta l'angolo di rotazione dell'oggetto, se l'oggetto è fermo anche l'angolo sarà pari a 0
     */
    public void setMotionAngle(float angle)
    {
        velocityVec.setAngle(angle);
    }

    /**
     * Questo metodo è comodo per far sì che un oggetto sia rivolto nella direzione in cui si sta muovendo.
     * Nel metodo act() si dovrà richiamare Action.setRotation(getMotionAngle())
     */
    public float getMotionAngle()
    {
        return velocityVec.angle();
    }

    /**
     *  Update accelerate vector by angle and value stored in acceleration field.
     *  Acceleration is applied by <code>applyPhysics</code> method.
     *  @param angle Angle (degrees) in which to accelerate.
     *  @see #acceleration
     *  @see #applyPhysics
     */
    public void accelerateAtAngle(float angle)
    {
        accelerationVec.add( 
            new Vector2(acceleration, 0).setAngle(angle) );
    }

    /**
     *  Aagiorna il vettore di accelerazione in base all'angolo di rotazione corrente e al valore memorizzato nel campo di accelerazione.
     * * L'accelerazione è applicata dal metodo applyPhysics.
     */
    public void accelerateForward()
    {

        accelerateAtAngle( getRotation() );
    }

    /**
     *  Metodo che si occupa di:
     *  Regolare il vettore velocità in base al vettore accelerazione
     *  COntrolla che la velocità non sia superiore a quella massima
     *  Se l'oggetto non accelera allora imposta il valore di decelerazione
     *  Modifica la posizione dell'oggetto in base al vettore velocità
     *
     */
    public void applyPhysics(float dt)
    {
        // aggiunge l'accelerazione
        velocityVec.add( accelerationVec.x * dt, accelerationVec.y * dt );

        float speed = getSpeed();

        // decrementa la velocità se non si sta accelerando
        if (accelerationVec.len() == 0)
            speed -= deceleration * dt;

        // verifica che non si superi la velocità massima
        speed = MathUtils.clamp(speed, 0, maxSpeed);

        // aggiorna la velocità
        setSpeed(speed);

        // imposta la velocità
        moveBy( velocityVec.x * dt, velocityVec.y * dt );

        // reset accelerazione
        accelerationVec.set(0,0);
    }

    // ----------------------------------------------
    // Collision polygon methods
    // ----------------------------------------------

    /**
     * Imposta il rettangolo corrispondente all'immagine dell'oggetto
     */
    public void setBoundaryRectangle()
    {
        float w = getWidth();
        float h = getHeight(); 

        float[] vertices = {0,0, w,0, w,h, 0,h};
        boundaryPolygon = new Polygon(vertices);
    }

    /**
     * Imposta il poligono corrispondente all'immagine dell'oggetto
     * Sostituisce il rettangolo con un poligono per gestire le collisioni
     * Riceve come parametro il numero di lati del poligono e genera una forma vicina ad una ellisse che si adatta alla forma dell'oggetto
     * Importante: non richiamare questo metodo prima che venga impostata la dimensione dell'oggetto con il metodo
     * setSize (richiamato manualmente) o setAnimation (richiamato automaticamente)
     * perchè il metodo funziona correttamente solo se si hanno le dimensioni height e width
     * */
    public void setBoundaryPolygon(int numSides)
    {
        float w = getWidth();
        float h = getHeight();

        float[] vertices = new float[2*numSides];
        for (int i = 0; i < numSides; i++)
        {
            float angle = i * 6.28f / numSides;
            // x-coordinate
            vertices[2*i] = w/2 * MathUtils.cos(angle) + w/2;
            // y-coordinate
            vertices[2*i+1] = h/2 * MathUtils.sin(angle) + h/2;
        }
        boundaryPolygon = new Polygon(vertices);

    }

    /**
     *  Ritorna il poligono che identifica le dimensioni dell'oggetto anche in base al suo angolo di rotazione
     */
    public Polygon getBoundaryPolygon()
    {
        boundaryPolygon.setPosition( getX(), getY() );
        boundaryPolygon.setOrigin( getOriginX(), getOriginY() );
        boundaryPolygon.setRotation( getRotation() );
        boundaryPolygon.setScale( getScaleX(), getScaleY() );        
        return boundaryPolygon;
    }

    /**
     *  Verifica se ci sono collisioni controllando l'intersezione dei poligoni
     *
     */
    public boolean overlaps(BaseActor other)
    {
        Polygon poly1 = this.getBoundaryPolygon();
        Polygon poly2 = other.getBoundaryPolygon();

        // verifica iniziale, più veloce. Migliora le prestazioni
        if ( !poly1.getBoundingRectangle().overlaps(poly2.getBoundingRectangle()) )
            return false;

        return Intersector.overlapConvexPolygons( poly1, poly2 );
    }

    /**
     * Implementa un comportamento di tipo "solido":
     * quando c'è sovrapposizione, allontana questo BaseActor dall'altro BaseActor
     * lungo il vettore di traslazione minimo finché non c'è più sovrapposizione.
     * @param other BaseActor per controllare la sovrapposizione
     * @return vettore di direzione con cui l'attore è stato traslato, null se non c'è sovrapposizione
     *
     */
    public Vector2 preventOverlap(BaseActor other)
    {
        Polygon poly1 = this.getBoundaryPolygon();
        Polygon poly2 = other.getBoundaryPolygon();

        // verifica iniziale, migliora le performance
        if ( !poly1.getBoundingRectangle().overlaps(poly2.getBoundingRectangle()) )
            return null;

        MinimumTranslationVector mtv = new MinimumTranslationVector();
        boolean polygonOverlap = Intersector.overlapConvexPolygons(poly1, poly2, mtv);

        if ( !polygonOverlap )
            return null;

        this.moveBy( mtv.normal.x * mtv.depth, mtv.normal.y * mtv.depth );
        return mtv.normal;
    }

    /**
     *  Imposta le dimensioni del mondo, consente di utilizzare i metodi boundToWorld() e scrollTo()
     *  Set world dimensions for use by methods boundToWorld() and scrollTo().
     */
    public static void setWorldBounds(float width, float height)
    {
        worldBounds = new Rectangle( 0,0, width, height );
    }   

    /**
     *  Come prima ma accetta come parametro un BaseActor
     */
    public static void setWorldBounds(BaseActor ba)
    {
        setWorldBounds( ba.getWidth(), ba.getHeight() );
    }   

    /**
     * Utile quando un oggetto arriva ai confini del mondo, modifica la posizione mantenendolo entro i limiti
     */
    public void boundToWorld()
    {
        if (getX() < 0)
            setX(0);
        if (getX() + getWidth() > worldBounds.width)    
            setX(worldBounds.width - getWidth());
        if (getY() < 0)
            setY(0);
        if (getY() + getHeight() > worldBounds.height)
            setY(worldBounds.height - getHeight());
    }
    
    /**
     *  Center camera on this object, while keeping camera's range of view 
     *  (determined by screen size) completely within world bounds.
     */
    public void alignCamera()
    {
        Camera cam = this.getStage().getCamera();
        Viewport v = this.getStage().getViewport();

        // center camera on actor
        cam.position.set( this.getX() + this.getOriginX(), this.getY() + this.getOriginY(), 0 );

        // bound camera to layout
        cam.position.x = MathUtils.clamp(cam.position.x, cam.viewportWidth/2,  worldBounds.width -  cam.viewportWidth/2);
        cam.position.y = MathUtils.clamp(cam.position.y, cam.viewportHeight/2, worldBounds.height - cam.viewportHeight/2);
        cam.update();
    }
    
    // ----------------------------------------------
    // Metodi Liste di oggetti BaseActor
    // ----------------------------------------------

    /**
     *  Recupera un elenco di tutte le istanze dell'oggetto dalla fase specificata con il nome di classe specificato
     *  o la cui classe estende la classe con il nome specificato.
     *  Se non esiste alcuna istanza, restituisce un elenco vuoto.
     *  Utile quando si codificano interazioni tra diversi tipi di oggetti di gioco nel metodo di aggiornamento.
     *  @param stage Stage contenente tutte le istanze di BaseActor
     *  @param className il nome della classe che estende BaseActor
     *  @return lista degli oggetti richiesti
     */
    public static ArrayList<BaseActor> getList(Stage stage, String className)
    {
        ArrayList<BaseActor> list = new ArrayList<BaseActor>();
        
        Class theClass = null;
        try
        {  theClass = Class.forName(className);  }
        catch (Exception error)
        {
            error.printStackTrace();
            return list;
        }
        
        for (Actor a : stage.getActors())
        {
            if ( theClass.isInstance( a ) )
                list.add( (BaseActor)a );
        }

        return list;
    }

    /**
     *  Restituisce il numero di oggetti di una specifica istanza
     *  @param className il nome della classe che estende BaseActor
     *  @return numero di istanze della classe
     */
    public static int count(Stage stage, String className)
    {
        return getList(stage, className).size();
    }
    
    // ----------------------------------------------
    // Metodi act e draw
    // ----------------------------------------------

    /**
     *  Elabora tutte le azioni da eseguire sul pessonaggio,
     *  questo metodo viene richiamato automaticamente dal metodo act() della classe Stage.
     *  In questo metodo viene anche gestita la pausa del personaggio
     *  @param dt tempo in secondi prima di caricare il successivo frame
     */
    public void act(float dt)
    {
        super.act( dt );

        if (!animationPaused)
            elapsedTime += dt;
    }

    /**
     *  Questo metodo serve per stabilire l'immagine corretta da caricare durante l'animazione.
     *  Il metodo getKeyFrame prende l'immagine successiva
     *
     *  
     */
    public void draw(Batch batch, float parentAlpha) 
    {
        super.draw( batch, parentAlpha );


        Color c = getColor(); 
        batch.setColor(c.r, c.g, c.b, c.a);

        if ( animation != null && isVisible() )
            batch.draw( animation.getKeyFrame(elapsedTime), 
                getX(), getY(), getOriginX(), getOriginY(),
                getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation() );
    }

    public Animation<TextureRegion> getAnimation() {
        return animation;
    }

    public float getElapsedTime() {
        return elapsedTime;
    }

    /** Rettangolo del mondo di gioco (usato per limitare la telecamera). */
    public static Rectangle getWorldBounds() {
        return worldBounds;
    }
}