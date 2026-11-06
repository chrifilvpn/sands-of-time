import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;

/**
 * Avvio della versione per browser (TeaVM).
 * Il gioco occupa tutta la finestra del browser; la FitViewport mantiene le proporzioni 800x480.
 */
public class WebLauncher {

    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.width = 0;
        config.height = 0;
        config.antialiasing = false;
        // FreeType (font) nel browser ha bisogno del suo script
        config.preloadListener = assetLoader -> assetLoader.loadScript("freetype.js");
        new WebApplication(new GiocoPiramide(), config);
    }
}
