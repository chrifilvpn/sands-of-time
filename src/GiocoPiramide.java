/**
 * Classe principale del gioco: carica le risorse condivise e apre il menu.
 */
public class GiocoPiramide extends BaseGame
{
    @Override
    public void create()
    {
        super.create();
        Risorse.carica();
        setActiveScreen( new MenuScreen() );
    }

    @Override
    public void dispose()
    {
        super.dispose();
        Risorse.dispose();
        BaseActor.disposeTextures();
    }
}
