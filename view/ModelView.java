package view;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data;
    
    public ModelView(String view) {
        this.setView(view);
        this.data = new HashMap<>();
    }
    
    public ModelView() {
        this.data = new HashMap<>();
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
}