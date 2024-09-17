/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.io;

import java.io.InputStream;
import java.net.URL;

/**
 * A class to wrap access to multiple class loaders making them work as one
 *
 * @author Clinton Begin
 */
public class ClassLoaderWrapper {
  /**
   * 存在两个默认加载器 , 系统类加载器，或者叫app加载器
   */
    ClassLoader defaultClassLoader; //一般情况下位null
    ClassLoader systemClassLoader; // 在下面构造器的时候会赋值

    ClassLoaderWrapper() {
      try {
        systemClassLoader = ClassLoader.getSystemClassLoader();
      } catch (SecurityException ignored) {
        // AccessControlException on Google App Engine
      }
    }

  /**
   * Get a resource as a URL using the current class path
   *
   * @param resource
   *          - the resource to locate
   *
   * @return the resource or null
   */
  public URL getResourceAsURL(String resource) {
    return getResourceAsURL(resource, getClassLoaders(null));
  }

  /**
   * Get a resource from the classpath, starting with a specific class loader
   *
   * @param resource
   *          - the resource to find
   * @param classLoader
   *          - the first classloader to try
   *
   * @return the stream or null
   */
  public URL getResourceAsURL(String resource, ClassLoader classLoader) {
    return getResourceAsURL(resource, getClassLoaders(classLoader));
  }

  /**
   * Get a resource from the classpath
   *
   * @param resource
   *          - the resource to find
   *
   * @return the stream or null
   */
  public InputStream getResourceAsStream(String resource) {
    return getResourceAsStream(resource, getClassLoaders(null));
  }

  /**
   * Get a resource from the classpath, starting with a specific class loader
   *
   * @param resource
   *          - the resource to find
   * @param classLoader
   *          - the first class loader to try
   *
   * @return the stream or null
   */
  public InputStream getResourceAsStream(String resource, ClassLoader classLoader) {
    return getResourceAsStream(resource, getClassLoaders(classLoader));
  }

  /**
   * Find a class on the classpath (or die trying)
   *
   * @param name
   *          - the class to look for
   *
   * @return - the class
   *
   * @throws ClassNotFoundException
   *           Duh.
   */
  public Class<?> classForName(String name) throws ClassNotFoundException {
    return classForName(name, getClassLoaders(null));
  }

  /**
   * Find a class on the classpath, starting with a specific classloader (or die trying)
   *
   * @param name
   *          - the class to look for
   * @param classLoader
   *          - the first classloader to try
   *
   * @return - the class
   *
   * @throws ClassNotFoundException
   *           Duh.
   */
  public Class<?> classForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
    return classForName(name, getClassLoaders(classLoader));
  }

  /**
   * Try to get a resource from a group of classloaders
   *
   * @param resource
   *          - the resource to get
   * @param classLoader
   *          - the classloaders to examine
   *
   * @return the resource or null
   * 通过遍历类加载器获取对应的文件流
   */
  InputStream getResourceAsStream(String resource, ClassLoader[] classLoader) {
    for (ClassLoader cl : classLoader) {
      if (null != cl) {

        // try to find the resource as passed
        // 通过名称获取文件流 获取文件流的方式是通过JDK自带ClassLoader 中的根据文件名称获取文件路径，在根据路径转成的Stream流
        InputStream returnValue = cl.getResourceAsStream(resource);

        // now, some class loaders want this leading "/", so we'll add it and try again if we didn't find the resource
        //在不存在的情况下，再次尝试一下，即拼接一个 "/"再次尝试
        if (null == returnValue) {
          returnValue = cl.getResourceAsStream("/" + resource);
        }

        if (null != returnValue) {
          return returnValue;
        }
      }
    }
    return null;
  }

  /**
   * Get a resource as a URL using the current class path
   *
   * @param resource
   *          - the resource to locate
   * @param classLoader
   *          - the class loaders to examine
   *
   * @return the resource or null
   */
  URL getResourceAsURL(String resource, ClassLoader[] classLoader) {

    URL url;

    for (ClassLoader cl : classLoader) {

      if (null != cl) {

        // look for the resource as passed in...
        url = cl.getResource(resource);

        // ...but some class loaders want this leading "/", so we'll add it
        // and try again if we didn't find the resource
        if (null == url) {
          url = cl.getResource("/" + resource);
        }

        // "It's always in the last place I look for it!"
        // ... because only an idiot would keep looking for it after finding it, so stop looking already.
        if (null != url) {
          return url;
        }

      }

    }

    // didn't find it anywhere.
    return null;

  }

  /**
   * Attempt to load a class from a group of classloaders
   *
   * @param name
   *          - the class to load
   * @param classLoader
   *          - the group of classloaders to examine
   *
   * @return the class
   *
   * @throws ClassNotFoundException
   *           - Remember the wisdom of Judge Smails: Well, the world needs ditch diggers, too.
   */
  Class<?> classForName(String name, ClassLoader[] classLoader) throws ClassNotFoundException {

    for (ClassLoader cl : classLoader) {

      if (null != cl) {

        try {

          return Class.forName(name, true, cl);

        } catch (ClassNotFoundException e) {
          // we'll ignore this until all classloaders fail to locate the class
        }

      }

    }

    throw new ClassNotFoundException("Cannot find class: " + name);

  }

  ClassLoader[] getClassLoaders(ClassLoader classLoader) {
    return new ClassLoader[] {
            classLoader, defaultClassLoader, // 默认类加载器
            Thread.currentThread().getContextClassLoader(), // web 中可以使用，在java中也可以使用
        getClass().getClassLoader(),
            systemClassLoader // appClassLoader
    };
  }

}
