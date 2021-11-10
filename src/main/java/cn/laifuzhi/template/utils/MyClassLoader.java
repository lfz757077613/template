package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * 类加载分为装载 -> 连接(验证、准备、解析) -> 初始化
 * Class.forName()可以控制类是否初始化，也就是是否执行cinit，静态代码块和静态变量的初始化
 * ClassLoader.load()可以控制是否连接，如果连接的话该类中使用的其他类也会触发加载
 * 重写类加载器时，一般不用管连接，初始化的时候自然会连接
 * https://stackoverflow.com/questions/6638959/whats-the-difference-between-classloader-loadname-and-class-fornamename
 */
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

    // 打破双亲委派模型，重写loadClass。反之重写findClass。只有需要控制连接的自定义类加载器才需要重写loadClass(String, boolean)
    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(className)) {
            Class<?> c = null;
            try {
                c = javaseClassLoader.loadClass(className);
            } catch (ClassNotFoundException ignore){
            }
            if (c != null) {
                return c;
            }
            c = findLoadedClass(className);
            if (c != null) {
                return c;
            }
            return findClass(className);
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
