package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

@Slf4j
public class MyClassLoader extends URLClassLoader {
    private ClassLoader javaseClassLoader;

    public MyClassLoader() {
        super(new URL[0], null);
        javaseClassLoader = getSystemClassLoader();
        while (javaseClassLoader != null && javaseClassLoader.getParent() != null) {
            javaseClassLoader = javaseClassLoader.getParent();
        }
        if (javaseClassLoader == null) {
            throw new RuntimeException("javaseClassLoader null");
        }
    }

    // 打破双亲委派模型，重写loadClass。反之重写findClass
    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(className)) {
            Class<?> c = null;
            try {
                c = javaseClassLoader.loadClass(className);
            } catch (ClassNotFoundException ignore){
            }
            if (c != null) {
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
            c = findLoadedClass(className);
            if (c != null) {
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
            c = findClass(className);
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    public URL getResource(String name) {
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return super.findResources(name);
    }
}
