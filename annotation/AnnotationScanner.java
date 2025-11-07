package annotation;


import jakarta.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationScanner {

    public static class ScanResult {
        public final Map<String, Method> urlToMethod = new HashMap<>();
        public final Set<Class<?>> controllerClasses = new HashSet<>();
    }

    public static ScanResult scan(ServletContext ctx) {
        ScanResult result = new ScanResult();
        String classesPath = ctx.getRealPath("/WEB-INF/classes");
        if (classesPath == null) return result;

        File root = new File(classesPath);
        if (!root.exists()) return result;

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        scanDir(root, root, loader, result);
        return result;
    }

    private static void scanDir(File root, File current, ClassLoader loader, ScanResult result) {
        for (File file : Objects.requireNonNull(current.listFiles())) {
            if (file.isDirectory()) {
                scanDir(root, file, loader, result);
            } else if (file.getName().endsWith(".class")) {
                String className = toClassName(root, file);
                try {
                    Class<?> clazz = loader.loadClass(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        result.controllerClasses.add(clazz);

                        Controller ctrl = clazz.getAnnotation(Controller.class);
                        String base = ctrl.base();

                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(Route.class)) {
                                Route route = method.getAnnotation(Route.class);
                                String fullUrl = normalizeUrl(base + route.url());
                                result.urlToMethod.put(fullUrl, method);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String toClassName(File root, File file) {
        String absPath = file.getAbsolutePath();
        String rootPath = root.getAbsolutePath();
        String relative = absPath.substring(rootPath.length() + 1)
                .replace(File.separatorChar, '.')
                .replaceAll("\\.class$", "");
        return relative;
    }

    private static String normalizeUrl(String url) {
        url = url.replaceAll("//+", "/"); // supprime les doubles /
        if (!url.startsWith("/")) url = "/" + url;
        return url;
    }

    // --- Test ou démonstration ---
    // public static void printResult(ScanResult result) {
    //     System.out.println("=== Controllers détectés ===");
    //     for (Class<?> c : result.controllerClasses) {
    //         System.out.println("→ " + c.getName());
    //     }

    //     System.out.println("\n=== Routes mappées ===");
    //     for (Map.Entry<String, Method> e : result.urlToMethod.entrySet()) {
    //         System.out.println(e.getKey() + "  →  " + e.getValue().getDeclaringClass().getSimpleName() + "." + e.getValue().getName());
    //     }
    // }
}
