package src.framework;

import annotation.Authorized;
import annotation.Role;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * Gestionnaire d'autorisation pour vérifier l'authentification et les rôles des utilisateurs.
 */
public class AuthManager {
    private static final String AUTH_VAR;
    private static final String ROLE_VAR;
    
    static {
        String authVar = PropertiesUtil.get("auth.variable");
        String roleVar = PropertiesUtil.get("role.variable");
        
        // Valeurs par défaut si non définies dans application.properties
        AUTH_VAR = (authVar != null) ? authVar : "user";
        ROLE_VAR = (roleVar != null) ? roleVar : "userRole";
    }

    /**
     * Vérifie si l'utilisateur est autorisé à accéder à une méthode.
     * 
     * @param method la méthode à vérifier
     * @param req la requête HTTP contenant la session
     * @return true si l'utilisateur est autorisé, false sinon
     */
    public static boolean isAuthorized(Method method, HttpServletRequest req) {
        // Si la méthode n'a ni @Authorized ni @Role, elle est accessible
        if (!method.isAnnotationPresent(Authorized.class) && !method.isAnnotationPresent(Role.class)) {
            return true;
        }
        
        // Vérification @Authorized : l'utilisateur doit être authentifié
        if (method.isAnnotationPresent(Authorized.class)) {
            Object authValue = req.getSession(false) != null ? req.getSession(false).getAttribute(AUTH_VAR) : null;
            if (authValue == null) {
                return false;  // Non authentifié
            }
        }

        // Vérification @Role : l'utilisateur doit être authentifié ET avoir le rôle requis
        if (method.isAnnotationPresent(Role.class)) {
            // D'abord, vérifier l'authentification (nécessaire pour les rôles)
            Object authValue = req.getSession(false) != null ? req.getSession(false).getAttribute(AUTH_VAR) : null;
            if (authValue == null) {
                return false;  // Non authentifié, donc pas de rôle possible
            }

            Role roleAnnotation = method.getAnnotation(Role.class);
            String requiredRole = roleAnnotation.value();
            Object sessionRole = req.getSession(false).getAttribute(ROLE_VAR);
            if (sessionRole == null || !requiredRole.equals(sessionRole.toString())) {
                return false;  // Rôle insuffisant
            }
        }

        return true;  // Autorisé
    }
}
