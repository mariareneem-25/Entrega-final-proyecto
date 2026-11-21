import java.util.Map;

/** Serializador pequeño: la API recibe formularios y devuelve JSON sin dependencias. */
public final class JsonWeb {
    private JsonWeb() { }
    public static String escribir(Object valor) {
        if (valor == null) return "null";
        if (valor instanceof Boolean || valor instanceof Number) return valor.toString();
        if (valor instanceof Map<?, ?>) {
            StringBuilder s = new StringBuilder("{");
            ((Map<?, ?>) valor).forEach((k, v) -> {
                if (s.length() > 1) s.append(',');
                s.append(texto(k.toString())).append(':').append(escribir(v));
            });
            return s.append('}').toString();
        }
        if (valor instanceof Iterable<?>) {
            StringBuilder s = new StringBuilder("[");
            for (Object v : (Iterable<?>) valor) { if (s.length() > 1) s.append(','); s.append(escribir(v)); }
            return s.append(']').toString();
        }
        return texto(valor.toString());
    }
    private static String texto(String valor) {
        StringBuilder s = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"': s.append("\\\""); break;
                case '\\': s.append("\\\\"); break;
                case '\n': s.append("\\n"); break;
                case '\r': s.append("\\r"); break;
                case '\t': s.append("\\t"); break;
                default: if (c < 32) s.append(String.format("\\u%04x", (int) c)); else s.append(c);
            }
        }
        return s.append('"').toString();
    }
}
