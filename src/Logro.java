/**
 * Objetivo de aprendizaje que registra un estudiante: tiene un tema y puede marcarse como completado.
 */
public class Logro {

    private String tema;
    private boolean completado;

    public Logro(String tema) {
        this.tema = tema;
        this.completado = false;
    }

    public String getTema() {
        return tema;
    }

    public boolean isCompletado() {
        return completado;
    }

    public void marcarCompletado() {
        this.completado = true;
    }

    @Override
    public String toString() {
        return tema + " - " + (completado ? "Completado" : "Pendiente");
    }
}
