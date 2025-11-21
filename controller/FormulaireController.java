package controller;

import annotation.Controller;
import annotation.Route;
import annotation.RequestParam;
import annotation.Param;
import view.ModelView;

@Controller(base = "/formulaire")
public class FormulaireController {
    
    // Afficher le formulaire d'inscription
    @Route(url = "/inscription", method = "GET")
    public ModelView afficherFormulaire() {
        ModelView mv = new ModelView();
        mv.setView("/WEB-INF/views/inscription.jsp");
        return mv;
    }
    
    // Traiter le formulaire d'inscription
    @Route(url = "/inscription", method = "POST")
    public ModelView traiterInscription(
        @RequestParam("nom") String nom,
        @RequestParam("prenom") String prenom,
        @RequestParam("age") int age,
        @RequestParam("email") String email
    ) {
        ModelView mv = new ModelView();
        mv.setView("/WEB-INF/views/inscription-success.jsp");
        mv.addData("nom", nom);
        mv.addData("prenom", prenom);
        mv.addData("age", age);
        mv.addData("email", email);
        mv.addData("message", "Inscription réussie pour " + prenom + " " + nom);
        return mv;
    }
    
    // Paramètre optionnel
    @Route(url = "/recherche", method = "GET")
    public ModelView recherche(
        @RequestParam("query") String query,
        @RequestParam(value = "page", required = false) Integer page
    ) {
        ModelView mv = new ModelView();
        mv.setView("/WEB-INF/views/recherche.jsp");
        mv.addData("query", query);
        mv.addData("page", page != null ? page : 1);
        return mv;
    }
    
    // Combinaison de @Param et @RequestParam
    @Route(url = "/profil/{id}/update", method = "POST")
    public ModelView updateProfil(
        @Param("id") int id,
        @RequestParam("nom") String nom,
        @RequestParam("email") String email,
        @RequestParam("age") int age
    ) {
        ModelView mv = new ModelView();
        mv.setView("/WEB-INF/views/profil-updated.jsp");
        mv.addData("userId", id);
        mv.addData("nom", nom);
        mv.addData("email", email);
        mv.addData("age", age);
        mv.addData("message", "Profil #" + id + " mis à jour");
        return mv;
    }
    
    // Formulaire de connexion
    @Route(url = "/login", method = "GET")
    public ModelView afficherLogin() {
        return new ModelView("/WEB-INF/views/login.jsp");
    }
    
    @Route(url = "/login", method = "POST")
    public ModelView traiterLogin(
        @RequestParam("username") String username,
        @RequestParam("password") String password
    ) {
        ModelView mv = new ModelView();
        
        // Simulation de validation
        if (username.equals("admin") && password.equals("admin123")) {
            mv.setView("/WEB-INF/views/dashboard.jsp");
            mv.addData("user", username);
            mv.addData("message", "Bienvenue " + username);
        } else {
            mv.setView("/WEB-INF/views/login.jsp");
            mv.addData("error", "Identifiants incorrects");
        }
        
        return mv;
    }
}
