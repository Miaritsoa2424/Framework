package view;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;

public class FileUpload {
    private Part part;
    
    public FileUpload(Part part) {
        this.part = part;
    }
    
    public String getFileName() {
        String contentDisposition = part.getHeader("content-disposition");
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
    
    public String getContentType() {
        return part.getContentType();
    }
    
    public long getSize() {
        return part.getSize();
    }
    
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }
    
    public void write(String fileName) throws IOException {
        part.write(fileName);
    }
    
    public Part getPart() {
        return part;
    }
}
