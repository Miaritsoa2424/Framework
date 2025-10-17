package src.framework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.ServerException;

@WebServlet(name = "FrontFramework", urlPatterns = { "/" })
public class FrontFramework extends HttpServlet {

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
                    // Configuration du type de contenu et de l'encodage
                    resp.setContentType("text/html");
                    resp.setCharacterEncoding("UTF-8");
        
                    // Récupérer le writer pour écrire la réponse
                    PrintWriter out = resp.getWriter();
        
                    // Obtenir l'URL de la requête
                    String requestURL = req.getRequestURL().toString();
        
                    // Envoyer une réponse HTML complète
                    out.println("<!DOCTYPE html>");
                    out.println("<html><body>");
                    out.println("<h1>Framework Test</h1>");
                    out.println("<p>Requête GET reçue pour l'URL: " + requestURL + "</p>");
                    out.println("</body></html>");
                    out.flush();
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        RequestDispatcher defaultDispatcher = getServletContext().getNamedDispatcher("default");
        defaultDispatcher.forward(req, resp);

    }
}
