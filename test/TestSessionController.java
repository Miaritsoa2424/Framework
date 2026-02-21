package test;

import annotation.Controller;
import annotation.PostMapping;
import annotation.GetMapping;
import annotation.RequestParam;
import annotation.SessionParam;
import annotation.Session;
import view.ModelView;
import view.CustomSession;

@Controller(base = "/session")
public class TestSessionController {
    
    // ==================== Champs annotés @Session ====================
    // Ces champs sont automatiquement synchronisés avec la session HTTP
    
    @Session("currentUser")
    private String username;
    
    @Session("isLoggedIn")
    private Boolean loggedIn;
    
    @Session  // Utilise le nom du champ comme clé: "loginTime"
    private Long loginTime;
    
    // ==================== Méthodes du Controller ====================
    
    /**
     * Afficher le formulaire de connexion
     */
    @GetMapping("/login")
    public ModelView showLoginForm() {
        ModelView mv = new ModelView("login-form.jsp");
        return mv;
    }
    
    /**
     * Traiter la connexion en utilisant les champs @Session
     * Les modifications des champs sont automatiquement sauvegardées dans la session
     */
    @PostMapping("/login")
    public ModelView processLogin(
        @RequestParam("username") String inputUsername,
        @RequestParam("password") String password
    ) {
        ModelView mv = new ModelView();
        
        // Vérification simplifiée
        if (inputUsername != null && !inputUsername.isEmpty() && "admin".equals(password)) {
            // Modifier les champs @Session - ils seront automatiquement sauvegardés
            this.username = inputUsername;
            this.loggedIn = true;
            this.loginTime = System.currentTimeMillis();
            
            mv.setView("dashboard.jsp");
            mv.addData("message", "Bienvenue " + inputUsername + "!");
        } else {
            mv.setView("login-form.jsp");
            mv.addData("error", "Identifiants invalides");
        }
        
        return mv;
    }
    
    /**
     * Page du dashboard - Utilise @SessionParam pour obtenir des valeurs de session
     */
    @GetMapping("/dashboard")
    public ModelView showDashboard(
        @SessionParam("currentUser") String user,
        @SessionParam("isLoggedIn") Boolean isLogged,
        @SessionParam("loginTime") Long time
    ) {
        ModelView mv = new ModelView();
        
        if (isLogged != null && isLogged) {
            mv.setView("dashboard.jsp");
            mv.addData("username", user);
            mv.addData("loginTime", time);
        } else {
            mv.setView("login-form.jsp");
            mv.addData("error", "Vous devez vous connecter");
        }
        
        return mv;
    }
    
    /**
     * Alternative: Utiliser les champs @Session directement
     * Les valeurs sont injectées automatiquement avant l'exécution
     */
    @GetMapping("/dashboard-alt")
    public ModelView showDashboardAlt() {
        ModelView mv = new ModelView();
        
        // Les champs @Session sont déjà remplis avec les valeurs de la session
        if (this.loggedIn != null && this.loggedIn) {
            mv.setView("dashboard.jsp");
            mv.addData("username", this.username);
            mv.addData("loginTime", this.loginTime);
        } else {
            mv.setView("login-form.jsp");
            mv.addData("error", "Vous devez vous connecter");
        }
        
        return mv;
    }
    
    /**
     * Déconnexion - Mettre les champs @Session à null pour les supprimer
     */
    @GetMapping("/logout")
    public ModelView logout() {
        ModelView mv = new ModelView("login-form.jsp");
        
        // Mettre les champs à null = suppression de la session
        this.username = null;
        this.loggedIn = null;
        this.loginTime = null;
        
        mv.addData("message", "Vous avez été déconnecté");
        return mv;
    }
    
    /**
     * Alternative: Déconnexion via ModelView
     */
    @GetMapping("/logout-alt")
    public ModelView logoutAlternative() {
        ModelView mv = new ModelView("login-form.jsp");
        
        // Supprimer les variables de session via ModelView
        mv.removeSession("currentUser");
        mv.removeSession("isLoggedIn");
        mv.removeSession("loginTime");
        
        mv.addData("message", "Vous avez été déconnecté");
        return mv;
    }
    
    /**
     * Afficher les informations de session avec @SessionParam
     */
    @GetMapping("/info")
    public ModelView sessionInfo(
        @SessionParam("currentUser") String user,
        @SessionParam("isLoggedIn") Boolean isLogged,
        @SessionParam("loginTime") Long time
    ) {
        ModelView mv = new ModelView("session-info.jsp");
        
        mv.addData("currentUser", user);
        mv.addData("isLoggedIn", isLogged);
        mv.addData("loginTime", time);
        
        return mv;
    }
    
    /**
     * Ajouter une variable de session via les champs @Session
     * Ici on utilise CustomSession pour les cas plus dynamiques
     */
    @PostMapping("/add")
    public ModelView addSessionVariable(
        @RequestParam("key") String key,
        @RequestParam("value") String value,
        CustomSession session
    ) {
        ModelView mv = new ModelView("session-info.jsp");
        
        session.add(key, value);
        
        mv.addData("message", "Variable '" + key + "' ajoutée à la session");
        mv.addData("sessionData", session.getAllData());
        
        return mv;
    }
    
    /**
     * Supprimer une variable de session
     */
    @PostMapping("/remove")
    public ModelView removeSessionVariable(
        @RequestParam("key") String key,
        CustomSession session
    ) {
        ModelView mv = new ModelView("session-info.jsp");
        
        if (session.contains(key)) {
            session.remove(key);
            mv.addData("message", "Variable '" + key + "' supprimée de la session");
        } else {
            mv.addData("error", "Variable '" + key + "' non trouvée dans la session");
        }
        
        mv.addData("sessionData", session.getAllData());
        
        return mv;
    }
}
