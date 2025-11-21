package src.framework;

import annotation.Controller;
import annotation.Route;
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

        // --- Affichage console (au démarrage) ---
        System.out.println("=== Initialisation du FrontFramework ===");
        for (Class<?> c : scanResult.controllerClasses) {
            Controller ctrl = c.getAnnotation(Controller.class);
            System.out.println("Contrôleur: " + c.getName() + " | base=" + ctrl.base());
        }
        for (UrlPattern pattern : scanResult.urlPatterns) {
            Method m = pattern.getMethod();
            Route route = m.getAnnotation(Route.class);
            System.out.println("→ Pattern: " + pattern.getPattern() +
                    " | Classe: " + m.getDeclaringClass().getSimpleName() +
                    " | Méthode: " + m.getName() +
                    " | HTTP: " + route.method() +
                    " | Params: " + pattern.getParamNames());
        }
        System.out.println("=========================================");
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
        Map<String, String> urlParams = new HashMap<>();
        
        // 1. Chercher d'abord une correspondance exacte
        method = scanResult.urlToMethod.get(url);
        
        // 2. Si pas de correspondance exacte, chercher un pattern
        if (method == null) {
            for (UrlPattern pattern : scanResult.urlPatterns) {
                if (pattern.matches(url)) {
                    method = pattern.getMethod();
                    urlParams = pattern.extractParams(url);
                    break;
                }
            }
        }
        
        if (method == null) {
            throw new Exception("URL non trouvée: " + url);
        }

        Route route = method.getAnnotation(Route.class);
        if (!route.method().equalsIgnoreCase(httpMethod)) {
            throw new Exception("Méthode HTTP non autorisée. Attendu: " + route.method() + ", Reçu: " + httpMethod);
        }

        Class<?> controllerClass = method.getDeclaringClass();
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        
        // Préparer les arguments de la méthode
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            // Gestion des paramètres URL (@Param)
            if (param.isAnnotationPresent(annotation.Param.class)) {
                annotation.Param paramAnnotation = param.getAnnotation(annotation.Param.class);
                String paramName = paramAnnotation.value();
                String paramValue = urlParams.get(paramName);
                
                args[i] = convertParameter(paramValue, param.getType());
            } 
            // Gestion des paramètres de requête (@RequestParam)
            else if (param.isAnnotationPresent(annotation.RequestParam.class)) {
                annotation.RequestParam requestParam = param.getAnnotation(annotation.RequestParam.class);
                String paramName = requestParam.value();
                String paramValue = req.getParameter(paramName);
                
                // Vérifier si le paramètre est requis
                if (requestParam.required() && (paramValue == null || paramValue.trim().isEmpty())) {
                    throw new Exception("Paramètre requis manquant: " + paramName);
                }
                
                args[i] = convertParameter(paramValue, param.getType());
            } 
            else {
                args[i] = null;
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
    
    private Object convertParameter(String value, Class<?> targetType) {
        if (value == null) {
            return getDefaultValue(targetType);
        }
        
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return value.trim().isEmpty() ? 0 : Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return value.trim().isEmpty() ? 0L : Long.parseLong(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return value.trim().isEmpty() ? 0.0 : Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return value.trim().isEmpty() ? 0.0f : Float.parseFloat(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else {
                return value;
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Impossible de convertir '" + value + "' en " + targetType.getSimpleName());
        }
    }
    
    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        return null;
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
        defaultDispatcher.forward(req, resp);
    }
}
