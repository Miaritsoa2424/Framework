package view;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import java.util.Set;

/**
 * Classe de gestion des sessions personnalisée
 * Permet d'accéder et manipuler les variables de session depuis les controllers
 */
public class CustomSession {
    private Map<String, Object> sessionData;
    private HttpSession httpSession;
    
    /**
     * Constructeur qui initialise la session à partir de la session HTTP
     * @param httpSession La session HTTP de la requête
     */
    public CustomSession(HttpSession httpSession) {
        this.httpSession = httpSession;
        this.sessionData = new HashMap<>();
        copyFromHttpSession();
    }
    
    /**
     * Copie toutes les variables de la session HTTP vers le map local
     */
    private void copyFromHttpSession() {
        if (httpSession != null) {
            Enumeration<String> attributeNames = httpSession.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                Object value = httpSession.getAttribute(name);
                sessionData.put(name, value);
            }
        }
    }
    
    /**
     * Ajoute ou met à jour une variable de session
     * @param key La clé de la variable
     * @param value La valeur de la variable
     */
    public void add(String key, Object value) {
        sessionData.put(key, value);
        if (httpSession != null) {
            httpSession.setAttribute(key, value);
        }
    }
    
    /**
     * Récupère une variable de session
     * @param key La clé de la variable
     * @return La valeur de la variable ou null si non trouvée
     */
    public Object get(String key) {
        return sessionData.get(key);
    }
    
    /**
     * Récupère une variable de session avec un type spécifique
     * @param key La clé de la variable
     * @param type Le type attendu
     * @return La valeur castée ou null si non trouvée
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = sessionData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Supprime une variable de session
     * @param key La clé de la variable à supprimer
     */
    public void remove(String key) {
        sessionData.remove(key);
        if (httpSession != null) {
            httpSession.removeAttribute(key);
        }
    }
    
    /**
     * Vérifie si une variable de session existe
     * @param key La clé de la variable
     * @return true si la variable existe, false sinon
     */
    public boolean contains(String key) {
        return sessionData.containsKey(key);
    }
    
    /**
     * Retourne toutes les clés des variables de session
     * @return Un Set contenant toutes les clés
     */
    public Set<String> getKeys() {
        return sessionData.keySet();
    }
    
    /**
     * Retourne toutes les données de session sous forme de Map
     * @return Une copie du Map des données de session
     */
    public Map<String, Object> getAllData() {
        return new HashMap<>(sessionData);
    }
    
    /**
     * Supprime toutes les variables de session
     */
    public void clear() {
        if (httpSession != null) {
            for (String key : sessionData.keySet()) {
                httpSession.removeAttribute(key);
            }
        }
        sessionData.clear();
    }
    
    /**
     * Invalide la session (déconnexion)
     */
    public void invalidate() {
        sessionData.clear();
        if (httpSession != null) {
            httpSession.invalidate();
        }
    }
    
    /**
     * Retourne l'ID de la session
     * @return L'ID de la session HTTP ou null
     */
    public String getId() {
        return httpSession != null ? httpSession.getId() : null;
    }
    
    /**
     * Retourne le nombre de variables de session
     * @return Le nombre de variables
     */
    public int size() {
        return sessionData.size();
    }
    
    /**
     * Vérifie si la session est vide
     * @return true si aucune variable de session, false sinon
     */
    public boolean isEmpty() {
        return sessionData.isEmpty();
    }
}
