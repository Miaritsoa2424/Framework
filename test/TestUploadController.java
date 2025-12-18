package test;

import annotation.Controller;
import annotation.PostMapping;
import annotation.GetMapping;
import annotation.RequestParam;
import view.ModelView;
import view.FileUpload;
import java.util.Map;
import java.util.List;

@Controller(base = "/upload")
public class TestUploadController {
    
    // Afficher le formulaire d'upload
    @GetMapping("/form")
    public ModelView showUploadForm() {
        ModelView mv = new ModelView("upload-form.jsp");
        return mv;
    }
    
    // Test 1: Upload avec Map<String, Object>
    @PostMapping("/single")
    public ModelView uploadSingle(Map<String, Object> params) {
        ModelView mv = new ModelView("upload-result.jsp");
        
        String nom = (String) params.get("nom");
        String description = (String) params.get("description");
        FileUpload fichier = (FileUpload) params.get("fichier");
        
        mv.addData("nom", nom);
        mv.addData("description", description);
        
        if (fichier != null) {
            mv.addData("fileName", fichier.getFileName());
            mv.addData("fileSize", fichier.getSize());
            mv.addData("contentType", fichier.getContentType());
        }
        
        return mv;
    }
    
    // Test 2: Upload avec @RequestParam
    @PostMapping("/withparam")
    public ModelView uploadWithParam(
        @RequestParam("nom") String nom,
        @RequestParam("fichier") FileUpload fichier
    ) {
        ModelView mv = new ModelView("upload-result.jsp");
        
        mv.addData("nom", nom);
        
        if (fichier != null) {
            mv.addData("fileName", fichier.getFileName());
            mv.addData("fileSize", fichier.getSize());
            mv.addData("contentType", fichier.getContentType());
        }
        
        return mv;
    }
    
    // Test 3: Upload multiple fichiers avec Map
    @PostMapping("/multiple")
    public ModelView uploadMultiple(Map<String, Object> params) {
        ModelView mv = new ModelView("upload-result.jsp");
        
        String nom = (String) params.get("nom");
        mv.addData("nom", nom);
        
        Object fichiers = params.get("fichiers");
        
        if (fichiers instanceof List) {
            @SuppressWarnings("unchecked")
            List<FileUpload> fileList = (List<FileUpload>) fichiers;
            mv.addData("fileCount", fileList.size());
            mv.addData("files", fileList);
        } else if (fichiers instanceof FileUpload) {
            mv.addData("fileCount", 1);
            mv.addData("fileName", ((FileUpload) fichiers).getFileName());
        }
        
        return mv;
    }
    
    // Test 4: Upload avec List<FileUpload>
    @PostMapping("/list")
    public ModelView uploadList(
        @RequestParam("nom") String nom,
        @RequestParam("fichiers") List<FileUpload> fichiers
    ) {
        ModelView mv = new ModelView("upload-result.jsp");
        
        mv.addData("nom", nom);
        mv.addData("fileCount", fichiers.size());
        mv.addData("files", fichiers);
        
        return mv;
    }
}
