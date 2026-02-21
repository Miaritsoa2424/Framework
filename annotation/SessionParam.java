package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour injecter une variable de session dans un paramètre de méthode
 * 
 * Exemple d'utilisation:
 * @GetMapping("/dashboard")
 * public ModelView dashboard(@SessionParam("user") String username) {
 *     // username contient la valeur de session "user"
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SessionParam {
    /**
     * Le nom de la variable de session à injecter
     */
    String value();
}
