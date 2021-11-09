package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

@Slf4j
public class MyClassLoader extends URLClassLoader {
    private ClassLoader javaseClassLoader;

    public MyClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        javaseClassLoader = getSystemClassLoader();
        while (javaseClassLoader != null && javaseClassLoader.getParent() != null) {
            javaseClassLoader = javaseClassLoader.getParent();
        }
        if (javaseClassLoader == null) {
            throw new RuntimeException("javaseClassLoader null");
        }
    }

    public void addFile(File file) {
        try {
            addURL(file.getCanonicalFile().toURI().toURL());
        } catch (IOException e) {
            log.error("addFile error", e);
        }
    }

    // 打破双亲委派模型，重写loadClass。反之重写findClass
    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(className)) {
            Class<?> c = javaseClassLoader.loadClass(className);
            if (c != null) {
                return c;
            }
            c = findClass(className);
            if (c == null) {
                throw new ClassNotFoundException(className);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
