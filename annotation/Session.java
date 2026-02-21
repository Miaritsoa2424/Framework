package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer un champ du controller comme variable de session
 * Le champ sera automatiquement synchronisé avec la session HTTP
 * 
 * Exemple d'utilisation:
 * @Controller(base = "/user")
 * public class UserController {
 *     
 *     @Session("currentUser")
 *     private String username;
 *     
 *     @Session("isLoggedIn")
 *     private Boolean loggedIn;
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Session {
    /**
     * Le nom de la variable de session
     * Si vide, utilise le nom du champ
     */
    String value() default "";
}
