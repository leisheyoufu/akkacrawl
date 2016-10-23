package com.lei.akkacrawl.parser;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.lei.akkacrawl.TaskEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ParserModule implements Module {
    static Logger logger = LogManager.getLogger(ParserModule.class.getName());
    public static final String packageName = "com.lei.akkacrawl.parser";
    private String className;
    private static Map<String, String> mapping = new HashMap();

    public ParserModule(String domain) {
        this.className = mapping.get(domain);
    }

    @Override
    public void configure(Binder binder) {
        try {
            binder.bind(Service.class).to((Class<? extends Service>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            logger.error(e);
            System.exit(1);
        }
    }

    static {

        String packagePath = null;
        // packagePath = Class.class.getClass().getResource("/").getPath();
        packagePath = ParserModule.class.getResource("").getFile();
        logger.info("packagePath: "+ packagePath);
        //findAndAddClassesInPackageByFile(packageName, packagePath, false, classSet);
        Set<Class<?>> classSet = getClasses(packageName);
        for (Class item : classSet) {
            try {
                mapping.put((String) item.getField("domain").get(item), item.getName());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e);
            }
        }
    }

    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath,
                                                        final boolean recursive, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.info("Could not find any class under " + packageName);
            return;
        }
        File[] dirFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });
        for (File file : dirFiles) {
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "."
                                + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                addParserClass(className, classes);
            }
        }
    }

    private static void addParserClass(String className, Set<Class<?>> classes) {
        try {
            //classes.add(Class.forName(packageName + '.' + className));
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className);
            if (!clazz.isInterface()) {
                try {
                    clazz.getDeclaredMethod("process", TaskEntry.class, String.class, String.class);
                    clazz.getField("domain");
                    classes.add(clazz);
                } catch (NoSuchMethodException | NoSuchFieldException e) {
                    logger.error(e);
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error(e);
        }
    }

    public static Set<Class<?>> getClasses(String pack) {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        boolean recursive = false;
        String packageName = pack;
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(
                    packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClassesInPackageByFile(packageName, filePath,
                            recursive, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection())
                                .getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.charAt(0) == '/') {
                                name = name.substring(1);
                            }
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                if (idx != -1) {
                                    packageName = name.substring(0, idx)
                                            .replace('/', '.');
                                }
                                if ((idx != -1) || recursive) {
                                    if (name.endsWith(".class")
                                            && !entry.isDirectory()) {
                                        String className = name.substring(
                                                packageName.length() + 1, name
                                                        .length() - 6);
                                        addParserClass(className, classes);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }

        return classes;
    }
}
