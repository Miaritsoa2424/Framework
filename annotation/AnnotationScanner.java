package annotation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AnnotationScanner {

    public static List<Class<?>> findControllers(String packageName) {
        List<Class<?>> controllers = new ArrayList<>();
        try {
            String path = packageName.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);

            if (resource == null) {
                System.err.println("Package introuvable: " + packageName);
                return controllers;
            }

            File directory = new File(resource.toURI());
            if (!directory.exists()) {
                return controllers;
            }

            for (File file : directory.listFiles()) {
                if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(Controller.class)) {
                            controllers.add(clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return controllers;
    }

    public static void main(String[] args) {
        List<Class<?>> controllers = findControllers("annotation.controllers"); // ton package
        for (Class<?> c : controllers) {
            Controller annotation = c.getAnnotation(Controller.class);
            System.out.println("Classe: " + c.getName() + ", base=" + annotation.base());
        }
    }
}
