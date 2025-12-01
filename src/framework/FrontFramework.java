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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.rmi.ServerException;
import java.util.Map;

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

    private void customServe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String httpMethod = req.getMethod();
        
        try {
            String result = invokeMethod(path, httpMethod);
            resp.setContentType("text/html; charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println(result);
            out.flush();
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

    private String invokeMethod(String url, String httpMethod) throws Exception {
        if (scanResult == null || scanResult.urlToMethod.isEmpty()) {
            throw new Exception("Aucune route configurée");
        }

        Method method = scanResult.urlToMethod.get(url);
        if (method == null) {
            throw new Exception("URL non trouvée: " + url);
        }

        Route route = method.getAnnotation(Route.class);
        if (!route.method().equalsIgnoreCase(httpMethod)) {
            throw new Exception("Méthode HTTP non autorisée. Attendu: " + route.method() + ", Reçu: " + httpMethod);
        }

        Class<?> controllerClass = method.getDeclaringClass();
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        
        Object result = method.invoke(controllerInstance);
        
        if (result instanceof String) {
            return (String) result;
        } else {
            throw new Exception("La méthode doit retourner un String");
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
        defaultDispatcher.forward(req, resp);
    }
}
