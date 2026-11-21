import java.io.PrintStream;
import java.text.Normalizer;

/**
 * Flujo de salida de la versión de consola que quita las tildes y los signos de apertura para que el texto se vea bien en terminales sin soporte de UTF-8.
 */
public class FiltroConsola extends PrintStream {

    public FiltroConsola(PrintStream salida) {
        super(salida, true);
    }

    @Override
    public void print(String texto) {
        if (texto == null) {
            super.print(texto);
            return;
        }

        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("\u00BF", "")
                .replace("\u00A1", "");
        super.print(sinTildes);
    }

    public static void activar() {
        System.setOut(new FiltroConsola(System.out));
    }
}
 