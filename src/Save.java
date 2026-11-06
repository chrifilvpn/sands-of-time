import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Salvataggio automatico della partita (all'inizio di ogni livello).
 * Oltre al livello e all'artefatto salva anche l'epoca, le morti e il tempo di gioco.
 */
public class Save
{
    private static final String PREF_NAME    = "sands_of_time_save";
    private static final String KEY_LEVEL    = "livello";
    private static final String KEY_ARTIFACT = "artefatto";
    private static final String KEY_PASSATO  = "passato";
    private static final String KEY_MORTI    = "morti";
    private static final String KEY_TEMPO    = "tempo";

    private static Preferences prefs()
    {
        return Gdx.app.getPreferences(PREF_NAME);
    }

    public static void salva(int livello, boolean hasArtifact, boolean passato, int morti, float tempo)
    {
        Preferences p = prefs();
        p.putInteger(KEY_LEVEL, livello);
        p.putBoolean(KEY_ARTIFACT, hasArtifact);
        p.putBoolean(KEY_PASSATO, passato);
        p.putInteger(KEY_MORTI, morti);
        p.putFloat(KEY_TEMPO, tempo);
        p.flush();
    }

    /** true se esiste una partita salvata valida (il livello salvato esiste ancora). */
    public static boolean esiste()
    {
        Preferences p = prefs();
        if (!p.contains(KEY_LEVEL)) return false;
        int l = p.getInteger(KEY_LEVEL, 1);
        return l >= 1 && l <= Risorse.numeroLivelli;
    }

    public static int caricaLivello()
    {
        int l = prefs().getInteger(KEY_LEVEL, 1);
        if (l < 1 || l > Risorse.numeroLivelli) l = 1;
        return l;
    }

    public static boolean caricaArtefatto()
    {
        return prefs().getBoolean(KEY_ARTIFACT, false);
    }

    public static boolean caricaPassato()
    {
        return prefs().getBoolean(KEY_PASSATO, false);
    }

    public static int caricaMorti()
    {
        return prefs().getInteger(KEY_MORTI, 0);
    }

    public static float caricaTempo()
    {
        return prefs().getFloat(KEY_TEMPO, 0f);
    }

    /** Cancella il salvataggio (es. dopo aver finito il gioco). */
    public static void cancella()
    {
        Preferences p = prefs();
        p.clear();
        p.flush();
    }
}
