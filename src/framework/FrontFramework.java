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
import view.RestResponse;

import java.io.IOException;
import java.io.PrintWriter;
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
                if (paramType == String.class) {
                    args[i] = paramValue;
                } else if (paramType == int.class || paramType == Integer.class) {
                    args[i] = paramValue != null ? Integer.parseInt(paramValue) : 0;
                } else if (paramType == long.class || paramType == Long.class) {
                    args[i] = paramValue != null ? Long.parseLong(paramValue) : 0L;
                } else if (paramType == double.class || paramType == Double.class) {
                    args[i] = paramValue != null ? Double.parseDouble(paramValue) : 0.0;
                } else {
                    args[i] = paramValue;
                }
            } 
            // Si le paramètre n'a pas d'annotation, utiliser le nom du paramètre
            else if (param.isNamePresent()) {
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
                if (paramType == String.class) {
                    args[i] = paramValue;
                } else if (paramType == int.class || paramType == Integer.class) {
                    args[i] = paramValue != null ? Integer.parseInt(paramValue) : 0;
                } else if (paramType == long.class || paramType == Long.class) {
                    args[i] = paramValue != null ? Long.parseLong(paramValue) : 0L;
                } else if (paramType == double.class || paramType == Double.class) {
                    args[i] = paramValue != null ? Double.parseDouble(paramValue) : 0.0;
                } else if (paramType == boolean.class || paramType == Boolean.class) {
                    args[i] = paramValue != null ? Boolean.parseBoolean(paramValue) : false;
                } else {
                    args[i] = paramValue;
                }
            } else {
                args[i] = null;
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

    private void defaultServe(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
        defaultDispatcher.forward(req, resp);
    }
}
