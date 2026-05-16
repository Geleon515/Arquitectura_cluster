package arqui.grupo5.ui;

/**
 * Launcher separado de VendiaApp.
 *
 * En Java 17 + JavaFX, si la clase principal extiende Application
 * y contiene el main(), el módulo de JavaFX lanza un error al iniciar.
 * La solución estándar es tener esta clase Launcher que NO extiende
 * Application, y desde aquí llamar a VendiaApp.launch().
 */
public class Launcher {
    public static void main(String[] args) {
        VendiaApp.launch(VendiaApp.class, args);
    }
}
