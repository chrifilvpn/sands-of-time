/**
 * Statistiche della partita in corso (mostrate nella schermata finale).
 */
public class StatoPartita
{
    public static int morti = 0;
    public static float tempo = 0f;

    public static void nuova()
    {
        morti = 0;
        tempo = 0f;
    }

    public static void daSalvataggio()
    {
        morti = Save.caricaMorti();
        tempo = Save.caricaTempo();
    }

    /** Tempo formattato come mm:ss (oppure h:mm:ss). */
    public static String tempoFormattato()
    {
        int s = (int) tempo;
        int h = s / 3600, m = (s / 60) % 60, sec = s % 60;
        if (h > 0)
            return h + ":" + due(m) + ":" + due(sec);
        return due(m) + ":" + due(sec);
    }

    private static String due(int n)
    {
        return (n < 10 ? "0" : "") + n;
    }
}
