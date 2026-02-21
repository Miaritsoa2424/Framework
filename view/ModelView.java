package view;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data;
    private Map<String, Object> sessionToAdd;
    private Map<String, Boolean> sessionToRemove;
    
    public ModelView(String view) {
        this.setView(view);
        this.data = new HashMap<>();
        this.sessionToAdd = new HashMap<>();
        this.sessionToRemove = new HashMap<>();
    }
    
    public ModelView() {
        this.data = new HashMap<>();
        this.sessionToAdd = new HashMap<>();
        this.sessionToRemove = new HashMap<>();
    }
    
    public String getView() {
        return view;
    }
    
    public void setView(String view) {
        this.view = view;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    // Méthode pour ajouter une donnée
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
    
    // Méthode pour récupérer une donnée
    public Object getData(String key) {
        return this.data.get(key);
    }
    
    // ==================== Gestion des Sessions ====================
    
    /**
     * Ajoute une variable à la session
     * @param key La clé de la variable de session
     * @param value La valeur à stocker
     */
    public void addSession(String key, Object value) {
        this.sessionToAdd.put(key, value);
        // Si on ajoute une clé qu'on voulait supprimer, annuler la suppression
        this.sessionToRemove.remove(key);
    }
    
    /**
     * Supprime une variable de la session
     * @param key La clé de la variable à supprimer
     */
    public void removeSession(String key) {
        this.sessionToRemove.put(key, true);
        // Si on supprime une clé qu'on voulait ajouter, annuler l'ajout
        this.sessionToAdd.remove(key);
    }
    
    /**
     * Retourne les variables de session à ajouter
     * @return Map des variables à ajouter
     */
    public Map<String, Object> getSessionToAdd() {
        return sessionToAdd;
    }
    
    /**
     * Retourne les clés des variables de session à supprimer
     * @return Map des clés à supprimer
     */
    public Map<String, Boolean> getSessionToRemove() {
        return sessionToRemove;
    }
    
    /**
     * Vérifie s'il y a des modifications de session à appliquer
     * @return true s'il y a des modifications
     */
    public boolean hasSessionChanges() {
        return !sessionToAdd.isEmpty() || !sessionToRemove.isEmpty();
    }
}