package src.framework;

import java.io.InputStream;
import java.util.Properties;

/**
 * Classe utilitaire pour lire les propriétés depuis le fichier application.properties.
 */
public class PropertiesUtil {
    private static final Properties props = new Properties();

    static {
        try (InputStream is = PropertiesUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                // Log ou gestion d'erreur si le fichier n'existe pas
                System.err.println("application.properties non trouvé dans le classpath.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Récupère la valeur d'une propriété par sa clé.
     * @param key la clé de la propriété
     * @return la valeur de la propriété ou null si elle n'existe pas
     */
    public static String get(String key) {
        return props.getProperty(key);
    }
}
