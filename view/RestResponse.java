package view;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

public class RestResponse {
    private String status;
    private Object data;
    private ErrorDetail error;

    public RestResponse() {
    }

    public static RestResponse success(Object data) {
        RestResponse response = new RestResponse();
        response.status = "success";
        response.data = data;
        response.error = null;
        return response;
    }

    public static RestResponse error(String code, String message, Object details) {
        RestResponse response = new RestResponse();
        response.status = "error";
        response.data = null;
        response.error = new ErrorDetail(code, message, details);
        return response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{");
        json.append("\"status\":\"").append(status).append("\",");
        json.append("\"data\":").append(objectToJson(data)).append(",");
        json.append("\"error\":").append(error != null ? error.toJson() : "null");
        json.append("}");
        return json.toString();
    }

    private String objectToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        Class<?> clazz = obj.getClass();
        
        // Types primitifs et wrappers
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        // Map
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }
        
        // Collection (List, Set, etc.)
        if (obj instanceof Collection) {
            return collectionToJson((Collection<?>) obj);
        }
        
        // Tableaux
        if (clazz.isArray()) {
            return arrayToJson(obj);
        }
        
        // Objets personnalisés - utiliser la réflexion
        return customObjectToJson(obj);
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append(objectToJson(entry.getValue()));
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private String collectionToJson(Collection<?> collection) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Object item : collection) {
            if (!first) json.append(",");
            json.append(objectToJson(item));
            first = false;
        }
        json.append("]");
        return json.toString();
    }

    private String arrayToJson(Object array) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        if (array instanceof Object[]) {
            Object[] objArray = (Object[]) array;
            for (Object item : objArray) {
                if (!first) json.append(",");
                json.append(objectToJson(item));
                first = false;
            }
        } else if (array instanceof int[]) {
            int[] intArray = (int[]) array;
            for (int item : intArray) {
                if (!first) json.append(",");
                json.append(item);
                first = false;
            }
        } else if (array instanceof long[]) {
            long[] longArray = (long[]) array;
            for (long item : longArray) {
                if (!first) json.append(",");
                json.append(item);
                first = false;
            }
        } else if (array instanceof double[]) {
            double[] doubleArray = (double[]) array;
            for (double item : doubleArray) {
                if (!first) json.append(",");
                json.append(item);
                first = false;
            }
        } else if (array instanceof boolean[]) {
            boolean[] boolArray = (boolean[]) array;
            for (boolean item : boolArray) {
                if (!first) json.append(",");
                json.append(item);
                first = false;
            }
        }
        
        json.append("]");
        return json.toString();
    }

    private String customObjectToJson(Object obj) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        Class<?> clazz = obj.getClass();
        
        // Parcourir tous les champs (y compris privés)
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                // Ignorer les champs statiques et transient
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    
                    if (!first) json.append(",");
                    json.append("\"").append(field.getName()).append("\":");
                    
                    // Appel récursif pour gérer les objets imbriqués
                    json.append(objectToJson(value));
                    
                    first = false;
                } catch (IllegalAccessException e) {
                    // Ignorer les champs inaccessibles
                }
            }
            clazz = clazz.getSuperclass();
        }
        
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static class ErrorDetail {
        private String code;
        private String message;
        private Object details;

        public ErrorDetail(String code, String message, Object details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String toJson() {
            StringBuilder json = new StringBuilder("{");
            json.append("\"code\":\"").append(escapeJson(code)).append("\",");
            json.append("\"message\":\"").append(escapeJson(message)).append("\",");
            json.append("\"details\":");
            
            // Utiliser la même logique récursive pour details
            if (details == null) {
                json.append("null");
            } else if (details instanceof String) {
                json.append("\"").append(escapeJson(details.toString())).append("\"");
            } else {
                // Créer une instance temporaire pour utiliser objectToJson
                RestResponse temp = new RestResponse();
                json.append(temp.objectToJson(details));
            }
            
            json.append("}");
            return json.toString();
        }

        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r")
                      .replace("\t", "\\t");
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Object getDetails() {
            return details;
        }
    }
}
