package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour indiquer qu'une méthode nécessite un rôle spécifique.
 * L'utilisateur doit être authentifié et posséder le rôle requis pour accéder à cette méthode.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Role {
    /**
     * Le rôle requis pour accéder à la méthode.
     * @return le nom du rôle
     */
    String value();
}
