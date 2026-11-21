import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash con sal individual. Compatible con las cuentas antiguas durante su migración. */
public final class ClaveSegura {
    private static final String PREFIJO = "pbkdf2$";
    private static final int ITERACIONES = 210_000;
    private ClaveSegura() { }

    public static boolean esHash(String valor) {
        return valor != null && valor.startsWith(PREFIJO);
    }

    public static String proteger(String clave) {
        if (clave == null) throw new IllegalArgumentException("La contraseña es obligatoria.");
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return PREFIJO + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(derivar(clave, sal, ITERACIONES));
    }

    public static String asegurarHash(String valor) {
        return esHash(valor) ? valor : proteger(valor);
    }

    public static boolean verificar(String clave, String almacenada) {
        if (clave == null || almacenada == null) return false;
        if (!esHash(almacenada)) {
            return MessageDigest.isEqual(clave.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    almacenada.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            String[] partes = almacenada.split("\\$");
            int iteraciones = Integer.parseInt(partes[1]);
            if (partes.length != 4 || iteraciones < 10_000 || iteraciones > 1_000_000) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(partes[3]),
                    derivar(clave, Base64.getDecoder().decode(partes[2]), iteraciones));
        } catch (RuntimeException e) { return false; }
    }

    private static byte[] derivar(String clave, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("No se pudo proteger la contraseña.", e); }
        finally { spec.clearPassword(); }
    }
}
