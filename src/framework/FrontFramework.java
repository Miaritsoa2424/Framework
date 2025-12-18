package src.framework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import annotation.AnnotationScanner;
import view.ModelView;
import view.RestResponse;
import view.FileUpload;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.rmi.ServerException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "FrontFramework", urlPatterns = { "/" }, loadOnStartup = 1)
@MultipartConfig
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
            // Vérifier si c'est une erreur d'un RestController
            boolean isRestError = false;
            try {
                Method method = scanResult.urlToMethod.get(path);
                if (method == null) {
                    for (UrlPatternCheck pattern : scanResult.urlPatterns) {
                        if (pattern.matches(path)) {
                            method = pattern.getMethod();
                            break;
                        }
                    }
                }
                if (method != null) {
                    Class<?> controllerClass = method.getDeclaringClass();
                    isRestError = scanResult.restControllers.getOrDefault(controllerClass, false);
                }
            } catch (Exception ignored) {
            }
            
            if (isRestError) {
                resp.setContentType("application/json; charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                PrintWriter out = resp.getWriter();
                RestResponse errorResponse = RestResponse.error("NOT_FOUND", e.getMessage(), null);
                out.println(errorResponse.toJson());
                out.flush();
            } else {
                resp.setContentType("text/html; charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                PrintWriter out = resp.getWriter();
                out.println("<h1>Erreur 404</h1>");
                out.println("<p>URL non trouvée: " + path + "</p>");
                out.println("<p>Méthode HTTP: " + httpMethod + "</p>");
                out.flush();
            }
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
        
        // Vérifier si c'est un RestController
        boolean isRestController = scanResult.restControllers.getOrDefault(controllerClass, false);
        
        // Gérer les fichiers uploadés
        Map<String, List<FileUpload>> uploadedFiles = new HashMap<>();
        Map<String, FileUpload> singleFiles = new HashMap<>();
        
        try {
            Collection<Part> parts = req.getParts();
            for (Part part : parts) {
                String fieldName = part.getName();
                // Vérifier si c'est un fichier (a un filename)
                String fileName = getFileName(part);
                if (fileName != null && !fileName.isEmpty()) {
                    FileUpload fileUpload = new FileUpload(part);
                    
                    // Ajouter à la liste des fichiers multiples
                    uploadedFiles.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(fileUpload);
                    
                    // Garder aussi le premier fichier pour accès direct
                    if (!singleFiles.containsKey(fieldName)) {
                        singleFiles.put(fieldName, fileUpload);
                    }
                }
            }
        } catch (Exception e) {
            // Si la requête n'est pas multipart, continuer normalement
        }
        
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
                
                // Ajouter les fichiers uploadés au Map
                for (Map.Entry<String, List<FileUpload>> entry : uploadedFiles.entrySet()) {
                    List<FileUpload> files = entry.getValue();
                    if (files.size() == 1) {
                        paramMap.put(entry.getKey(), files.get(0));
                    } else {
                        paramMap.put(entry.getKey(), files);
                    }
                }
                
                args[i] = paramMap;
            }
            // Si le paramètre attend un FileUpload ou Part
            else if (paramType == FileUpload.class || paramType == Part.class) {
                String paramName = null;
                
                if (param.isAnnotationPresent(annotation.RequestParam.class)) {
                    annotation.RequestParam paramAnnotation = param.getAnnotation(annotation.RequestParam.class);
                    paramName = paramAnnotation.value();
                } else if (param.isNamePresent()) {
                    paramName = param.getName();
                }
                
                if (paramName != null && singleFiles.containsKey(paramName)) {
                    if (paramType == FileUpload.class) {
                        args[i] = singleFiles.get(paramName);
                    } else {
                        args[i] = singleFiles.get(paramName).getPart();
                    }
                } else {
                    args[i] = null;
                }
            }
            // Si le paramètre attend une List<FileUpload> ou List<Part>
            else if (paramType == List.class) {
                String paramName = null;
                
                if (param.isAnnotationPresent(annotation.RequestParam.class)) {
                    annotation.RequestParam paramAnnotation = param.getAnnotation(annotation.RequestParam.class);
                    paramName = paramAnnotation.value();
                } else if (param.isNamePresent()) {
                    paramName = param.getName();
                }
                
                if (paramName != null && uploadedFiles.containsKey(paramName)) {
                    args[i] = uploadedFiles.get(paramName);
                } else {
                    args[i] = new ArrayList<>();
                }
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
        
        // Si c'est un RestController, encapsuler dans RestResponse
        if (isRestController) {
            handleRestResponse(result, resp);
        } else {
            handleNormalResponse(result, req, resp);
        }
    }
    
    private void handleRestResponse(Object result, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        
        RestResponse restResponse;
        
        if (result instanceof RestResponse) {
            restResponse = (RestResponse) result;
        } else if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;
            restResponse = RestResponse.success(modelView.getData());
        } else {
            restResponse = RestResponse.success(result);
        }
        
        out.println(restResponse.toJson());
        out.flush();
    }
    
    private void handleNormalResponse(Object result, HttpServletRequest req, HttpServletResponse resp) 
            throws Exception, IOException, ServletException {
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

    /**
     * Extrait le nom du fichier depuis un Part
     */
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) return null;
        
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
        defaultDispatcher.forward(req, resp);
    }
}
