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

        // --- Affichage console (au démarrage) ---
        System.out.println("=== Initialisation du FrontFramework ===");
        for (Class<?> c : scanResult.controllerClasses) {
            Controller ctrl = c.getAnnotation(Controller.class);
            System.out.println("Contrôleur: " + c.getName() + " | base=" + ctrl.base());
        }
        for (Map.Entry<String, Method> entry : scanResult.urlToMethod.entrySet()) {
            Method m = entry.getValue();
            Route route = m.getAnnotation(Route.class);
            System.out.println("→ URL: " + entry.getKey() +
                    " | Classe: " + m.getDeclaringClass().getSimpleName() +
                    " | Méthode: " + m.getName() +
                    " | HTTP: " + route.method());
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

    private void customServe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html><body>");
        out.println("<h1>Framework Scan Result</h1>");

        if (scanResult == null || scanResult.controllerClasses.isEmpty()) {
            out.println("<p>Aucun contrôleur détecté.</p>");
        } else {
            out.println("<h2>Contrôleurs détectés :</h2>");
            for (Class<?> c : scanResult.controllerClasses) {
                Controller ctrl = c.getAnnotation(Controller.class);
                out.println("<p><strong>" + c.getName() + "</strong> (base = " + ctrl.base() + ")</p>");
            }

            out.println("<h2>Routes mappées :</h2>");
            out.println("<ul>");
            for (Map.Entry<String, Method> entry : scanResult.urlToMethod.entrySet()) {
                Method m = entry.getValue();
                Route route = m.getAnnotation(Route.class);
                out.println("<li><b>URL:</b> " + entry.getKey() +
                        " | <b>Classe:</b> " + m.getDeclaringClass().getSimpleName() +
                        " | <b>Méthode:</b> " + m.getName() +
                        " | <b>HTTP:</b> " + route.method() + "</li>");
            }
            out.println("</ul>");
        }

        out.println("</body></html>");
        out.flush();
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
        defaultDispatcher.forward(req, resp);
    }
}
