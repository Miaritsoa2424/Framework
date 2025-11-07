package annotation;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utilitaire de scan pour trouver les classes/sources annotées avec {@link Controller}.
 */
public final class AnnotationScanner {

    private AnnotationScanner() {}

    /**
     * Recherche toutes les classes du classpath sous le package fourni qui sont annotées avec {@link Controller}.
     * @param basePackage package de base à scanner (ex: "webframe")
     * @return la liste des noms de classes pleinement qualifiés trouvés
     */
    public static List<String> findClassesWithController(String basePackage) {
        if (basePackage == null || basePackage.trim().isEmpty()) {
            return Collections.emptyList();
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String packagePath = basePackage.replace('.', '/');
        Set<String> classNames = new HashSet<>();

        try {
            // 1) Ressources de type dossier sur le classpath
            Enumeration<URL> resources = cl.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    scanDirectoryURL(url, basePackage, classNames);
                } else if ("jar".equals(protocol)) {
                    scanJarURL(url, packagePath, classNames);
                }
            }

            // 2) Parcours complémentaire du classpath (dossiers et jars) pour robustesse
            String cp = System.getProperty("java.class.path");
            if (cp != null) {
                String[] entries = cp.split(java.io.File.pathSeparator);
                for (String entry : entries) {
                    File file = new File(entry);
                    if (file.isDirectory()) {
                        File baseDir = new File(file, packagePath);
                        if (baseDir.isDirectory()) {
                            scanDirectory(baseDir, basePackage, classNames);
                        }
                    } else if (entry.endsWith(".jar") && file.isFile()) {
                        try (JarFile jarFile = new JarFile(file)) {
                            scanJarFile(jarFile, packagePath, classNames);
                        } catch (IOException ignored) {
                            // On ignore les jars illisibles
                        }
                    }
                }
            }
        } catch (IOException e) {
            // En cas d'erreur d'accès aux ressources, on retourne ce qui a pu être trouvé
        }

        // Filtrer par annotation réellement présente
        List<String> annotated = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, cl);
                if (clazz.getAnnotation(Controller.class) != null) {
                    // exclure les classes internes synthétiques
                    if (clazz.getName().contains("$")) continue;
                    annotated.add(className);
                }
            } catch (Throwable ignored) {
                // Class non chargeable: on ignore
            }
        }

        Collections.sort(annotated);
        return annotated;
    }

    /**
     * Recherche tous les fichiers sources .java sous le dossier donné qui utilisent l'annotation Controller.
     * Heuristique: présence de "@Controller" avec import adéquat, ou "@webframe.core.annotation.Controller".
     * @param baseDir dossier racine (ex: src/main/java)
     * @return liste des fichiers sources correspondants
     */
    public static List<File> findSourceFilesWithController(File baseDir) {
        if (baseDir == null || !baseDir.isDirectory()) {
            return Collections.emptyList();
        }
        List<File> result = new ArrayList<>();
        collectJavaSources(baseDir, result);
        List<File> filtered = new ArrayList<>();
        for (File f : result) {
            try {
                // Lecture rapide du contenu
                String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                if (mentionsControllerAnnotation(content)) {
                    filtered.add(f);
                }
            } catch (IOException ignored) {
                // ignorer fichiers illisibles
            }
        }
        return filtered;
    }

    private static boolean mentionsControllerAnnotation(String content) {
        if (content == null) return false;
        // Simpliste mais suffisant pour la majorité des cas de test
        boolean hasSimple = content.contains("@Controller") &&
                (content.contains("import webframe.core.annotation.Controller") ||
                 content.contains("@webframe.core.annotation.Controller"));
        boolean hasFqn = content.contains("@webframe.core.annotation.Controller");
        return hasSimple || hasFqn;
    }

    private static void collectJavaSources(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectJavaSources(f, out);
            } else if (f.getName().endsWith(".java")) {
                out.add(f);
            }
        }
    }

    private static void scanDirectoryURL(URL url, String basePackage, Set<String> out) {
        try {
            // Décoder correctement les chemins (espaces, etc.)
            String decoded = URLDecoder.decode(url.getFile(), "UTF-8");
            File dir;
            try {
                dir = new File(new URI("file:" + decoded));
            } catch (Exception ex) {
                dir = new File(decoded);
            }
            if (dir.isDirectory()) {
                scanDirectory(dir, basePackage, out);
            }
        } catch (Exception e) {
            File dir = new File(url.getPath());
            if (dir.isDirectory()) {
                scanDirectory(dir, basePackage, out);
            }
        }
    }

    private static void scanDirectory(File baseDir, String currentPackage, Set<String> out) {
        File[] files = baseDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                String nextPackage = currentPackage + "." + file.getName();
                scanDirectory(file, nextPackage, out);
            } else if (file.getName().endsWith(".class")) {
                String simple = file.getName().substring(0, file.getName().length() - 6); // enlever .class
                String className = currentPackage + "." + simple;
                out.add(className);
            }
        }
    }

    private static void scanJarURL(URL url, String packagePath, Set<String> out) {
        try {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = conn.getJarFile()) {
                scanJarFile(jarFile, packagePath, out);
            }
        } catch (IOException | ClassCastException ignored) {
            // Pas un jar exploitable
        }
    }

    private static void scanJarFile(JarFile jarFile, String packagePath, Set<String> out) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            String name = e.getName();
            if (name.startsWith(packagePath) && name.endsWith(".class") && !name.contains("$")) {
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                out.add(className);
            }
        }
    }
}