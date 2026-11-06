import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

/**
 * Punto di avvio del gioco "Sands of Time".
 */
public class Launcher {
    public static void main(String[] args) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Sands of Time";
        config.width = 800;
        config.height = 480;
        config.resizable = true;        // la FitViewport gestisce il ridimensionamento
        config.vSyncEnabled = true;     // niente "tearing" durante lo scorrimento
        config.foregroundFPS = 60;
        config.backgroundFPS = 30;      // consuma meno quando la finestra non è attiva

        // icone in più dimensioni: il sistema operativo sceglie quella giusta
        config.addIcon("assets/logo64.png", Files.FileType.Internal);
        config.addIcon("assets/logo32.png", Files.FileType.Internal);
        config.addIcon("assets/logo16.png", Files.FileType.Internal);

        new LwjglApplication(new GiocoPiramide(), config);
    }
}
