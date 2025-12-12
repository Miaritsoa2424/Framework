package src.framework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import annotation.AnnotationScanner;
import view.ModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.rmi.ServerException;
import java.util.Map;
import java.util.HashMap;

@WebServlet(name = "FrontFramework", urlPatterns = { "/" }, loadOnStartup = 1)
public class FrontFramework extends HttpServlet {

    private AnnotationScanner.ScanResult scanResult;

    @Override
    public void init() throws ServletException {
        super.init();
        ServletContext ctx = getServletContext();
        scanResult = AnnotationScanner.scan(ctx);
        ctx.setAttribute("scanResult", scanResult);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServerException, IOException, ServletException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean ressourceExists = getServletContext().getResourceAsStream(path) != null;

        if (ressourceExists) {
            defaultServe(req, resp);
        } else {
            customServe(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServerException, IOException, ServletException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean ressourceExists = getServletContext().getResourceAsStream(path) != null;

        if (ressourceExists) {
            defaultServe(req, resp);
        } else {
            customServe(req, resp);
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String httpMethod = req.getMethod();
        
        try {
            invokeMethod(path, httpMethod, req, resp);
        } catch (Exception e) {
            resp.setContentType("text/html; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            PrintWriter out = resp.getWriter();
            out.println("<h1>Erreur 404</h1>");
            out.println("<p>URL non trouvée: " + path + "</p>");
            out.println("<p>Méthode HTTP: " + httpMethod + "</p>");
            out.flush();
        }
    }

    private void invokeMethod(String url, String httpMethod, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (scanResult == null || scanResult.urlToMethod.isEmpty()) {
            throw new Exception("Aucune route configurée");
        }

        Method method = null;
        Map<String, Object> urlParams = new HashMap<>();
        
        // 1. Chercher d'abord une correspondance exacte
        method = scanResult.urlToMethod.get(url);
        
        // 2. Si pas de correspondance exacte, chercher un pattern
        if (method == null) {
            for (UrlPatternCheck pattern : scanResult.urlPatterns) {
                if (pattern.matches(url)) {
                    method = pattern.getMethod();
                    Map<String, String> extractedParams = pattern.extractParams(url);
                    // Convertir Map<String, String> en Map<String, Object>
                    for (Map.Entry<String, String> entry : extractedParams.entrySet()) {
                        urlParams.put(entry.getKey(), entry.getValue());
                    }
                    break;
                }
            }
        }
        
        if (method == null) {
            throw new Exception("URL non trouvée: " + url);
        }

        String expectedHttpMethod = scanResult.methodToHttpMethod.get(method);
        if (!expectedHttpMethod.equalsIgnoreCase(httpMethod)) {
            throw new Exception("Méthode HTTP non autorisée. Attendu: " + expectedHttpMethod + ", Reçu: " + httpMethod);
        }

        Class<?> controllerClass = method.getDeclaringClass();
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        
        // Préparer les arguments de la méthode
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            
            // Si le paramètre est un Map<String, Object>
            if (paramType == Map.class) {
                Map<String, Object> paramMap = new HashMap<>();
                
                // Récupérer tous les paramètres de la requête HTTP
                Map<String, String[]> requestParams = req.getParameterMap();
                for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
                    String key = entry.getKey();
                    String[] values = entry.getValue();
                    
                    // Si un seul value, on met directement le String
                    // Sinon on met le tableau
                    if (values.length == 1) {
                        paramMap.put(key, values[0]);
                    } else {
                        paramMap.put(key, values);
                    }
                }
                
                // Ajouter aussi les paramètres extraits de l'URL
                for (Map.Entry<String, Object> entry : urlParams.entrySet()) {
                    paramMap.put(entry.getKey(), entry.getValue());
                }
                
                args[i] = paramMap;
            }
            // Si le paramètre a l'annotation @RequestParam
            else if (param.isAnnotationPresent(annotation.RequestParam.class)) {
                annotation.RequestParam paramAnnotation = param.getAnnotation(annotation.RequestParam.class);
                String paramName = paramAnnotation.value();
                String paramValue = null;
                
                // Chercher d'abord dans les paramètres URL extraits
                Object urlParamValue = urlParams.get(paramName);
                if (urlParamValue != null) {
                    paramValue = urlParamValue.toString();
                } else {
                    // Sinon chercher dans les paramètres de la requête
                    paramValue = req.getParameter(paramName);
                }
                
                // Conversion selon le type
                args[i] = convertValue(paramValue, paramType);
            }
            // Si le paramètre est un type primitif ou wrapper ou String
            else if (isPrimitiveOrWrapper(paramType) && param.isNamePresent()) {
                String paramName = param.getName();
                String paramValue = null;
                
                // Chercher d'abord dans les paramètres URL extraits
                Object urlParamValue = urlParams.get(paramName);
                if (urlParamValue != null) {
                    paramValue = urlParamValue.toString();
                } else {
                    // Sinon chercher dans les paramètres de la requête
                    paramValue = req.getParameter(paramName);
                }
                
                // Conversion selon le type
                args[i] = convertValue(paramValue, paramType);
            }
            // Si le paramètre est un objet personnalisé
            else {
                args[i] = createObjectFromRequest(paramType, req, urlParams);
            }
        }
        
        Object result = method.invoke(controllerInstance, args);
        
        if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;

            // Transférer toutes les données du ModelView dans la requête
            for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }

            String viewPath = modelView.getView();
            
            // Normaliser le chemin de la vue pour qu'il soit absolu
            if (!viewPath.startsWith("/")) {
                viewPath = "/" + viewPath;
            }
            
            RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
            dispatcher.forward(req, resp);
        } else if (result instanceof String) {
            resp.setContentType("text/html; charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println((String) result);
            out.flush();
        } else {
            throw new Exception("La méthode doit retourner un String ou un ModelView");
        }
    }

    /**
     * Vérifie si un type est primitif, wrapper ou String
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() 
            || type == String.class 
            || type == Integer.class 
            || type == Long.class 
            || type == Double.class 
            || type == Float.class 
            || type == Boolean.class 
            || type == Character.class 
            || type == Byte.class 
            || type == Short.class;
    }

    /**
     * Convertit une valeur String vers le type cible
     */
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                if (targetType == int.class) return 0;
                if (targetType == long.class) return 0L;
                if (targetType == double.class) return 0.0;
                if (targetType == float.class) return 0.0f;
                if (targetType == boolean.class) return false;
                if (targetType == char.class) return '\0';
                if (targetType == byte.class) return (byte) 0;
                if (targetType == short.class) return (short) 0;
            }
            return null;
        }

        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == char.class || targetType == Character.class) {
            return value.charAt(0);
        } else if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(value);
        } else if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value);
        }
        return value;
    }

    /**
     * Crée un objet à partir des paramètres de la requête
     */
    private Object createObjectFromRequest(Class<?> objectType, HttpServletRequest req, Map<String, Object> urlParams) throws Exception {
        // Créer une nouvelle instance de l'objet
        Object instance = objectType.getDeclaredConstructor().newInstance();
        
        // Récupérer tous les champs de la classe
        Field[] fields = objectType.getDeclaredFields();
        
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            
            // Chercher la valeur dans les paramètres de la requête
            String paramValue = req.getParameter(fieldName);
            
            // Si pas trouvé dans la requête, chercher dans les paramètres URL
            if (paramValue == null && urlParams.containsKey(fieldName)) {
                Object urlValue = urlParams.get(fieldName);
                paramValue = urlValue != null ? urlValue.toString() : null;
            }
            
            // Si on a trouvé une valeur, la convertir et l'assigner
            if (paramValue != null) {
                Object convertedValue = convertValue(paramValue, fieldType);
                field.set(instance, convertedValue);
            }
        }
        
        return instance;
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
        defaultDispatcher.forward(req, resp);
    }
}
