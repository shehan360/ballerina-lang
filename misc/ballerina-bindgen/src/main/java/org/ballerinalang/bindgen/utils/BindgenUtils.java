/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.bindgen.utils;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.ballerinalang.bindgen.exceptions.BindgenException;
import org.ballerinalang.bindgen.model.JField;
import org.ballerinalang.bindgen.model.JMethod;
import org.ballerinalang.bindgen.model.JParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.ballerinalang.bindgen.utils.BindgenConstants.BALLERINA_STRING;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BALLERINA_STRING_ARRAY;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BAL_EXTENSION;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BOOLEAN;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BOOLEAN_ARRAY;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BYTE;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BYTE_ARRAY;
import static org.ballerinalang.bindgen.utils.BindgenConstants.CHAR;
import static org.ballerinalang.bindgen.utils.BindgenConstants.DOUBLE;
import static org.ballerinalang.bindgen.utils.BindgenConstants.FLOAT;
import static org.ballerinalang.bindgen.utils.BindgenConstants.FLOAT_ARRAY;
import static org.ballerinalang.bindgen.utils.BindgenConstants.HANDLE;
import static org.ballerinalang.bindgen.utils.BindgenConstants.INT;
import static org.ballerinalang.bindgen.utils.BindgenConstants.INT_ARRAY;
import static org.ballerinalang.bindgen.utils.BindgenConstants.JAVA_STRING;
import static org.ballerinalang.bindgen.utils.BindgenConstants.JAVA_STRING_ARRAY;
import static org.ballerinalang.bindgen.utils.BindgenConstants.LONG;
import static org.ballerinalang.bindgen.utils.BindgenConstants.MUSTACHE_FILE_EXTENSION;
import static org.ballerinalang.bindgen.utils.BindgenConstants.SHORT;
import static org.ballerinalang.bindgen.utils.BindgenConstants.TEMPLATES_DIR_PATH_KEY;

/**
 * Class containing the util methods of the Ballerina Bindgen CLI tool.
 *
 * @since 1.2.0
 */
public class BindgenUtils {

    private BindgenUtils() {
    }

    private static final PrintStream errStream = System.err;
    private static final PrintStream outStream = System.out;

    public static void writeOutputFile(Object object, String templateDir, String templateName, String outPath,
                                       Boolean append) throws BindgenException {
        PrintWriter writer = null;
        FileWriterWithEncoding fileWriter = null;
        try {
            Template template = compileTemplate(templateDir, templateName);
            Context context = Context.newBuilder(object).resolver(
                    MapValueResolver.INSTANCE,
                    JavaBeanValueResolver.INSTANCE,
                    FieldValueResolver.INSTANCE).build();
            fileWriter = new FileWriterWithEncoding(outPath, StandardCharsets.UTF_8, append);
            writer = new PrintWriter(fileWriter);
            writer.println(template.apply(context));
            fileWriter.close();
        } catch (IOException e) {
            throw new BindgenException("Unable to create the Ballerina file: " + e.getMessage(), e);
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException ignored) {

                }
            }
        }
    }

    private static Template compileTemplate(String defaultTemplateDir, String templateName)
            throws BindgenException {
        String templatesDirPath = System.getProperty(TEMPLATES_DIR_PATH_KEY, defaultTemplateDir);
        ClassPathTemplateLoader cpTemplateLoader = new ClassPathTemplateLoader((templatesDirPath));
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(templatesDirPath);
        cpTemplateLoader.setSuffix(MUSTACHE_FILE_EXTENSION);
        fileTemplateLoader.setSuffix(MUSTACHE_FILE_EXTENSION);
        Handlebars handlebars = new Handlebars().with(cpTemplateLoader, fileTemplateLoader);

        // Helper to obtain a single quote escape character in front of Ballerina reserved words.
        handlebars.registerHelper("controlChars", (object, options) -> {
            if (object instanceof String) {
                return "\'" + object;
            }
            return "";
        });

        // Helper to obtain the simple class name from a fully qualified class name.
        handlebars.registerHelper("getSimpleName", (object, options) -> {
            if (object instanceof String) {
                String className = (String) object;
                return className.substring(className.lastIndexOf('.') + 1);
            }
            return object;
        });

        // Helper to obtain the parameters string with the required Ballerina to Java conversions.
        handlebars.registerHelper("getParams", (object, options) -> {
            String returnString = "";
            if (object instanceof JParameter) {
                JParameter param = (JParameter) object;
                returnString = getParamsHelper(param);
            }
            return returnString;
        });

        // Helper to obtain the returns string while analyzing the return types and errors.
        handlebars.registerHelper("getReturn", (object, options) -> {
            String returnString = "";
            if (object instanceof JMethod) {
                JMethod jMethod = (JMethod) object;
                returnString = getReturnHelper(jMethod);
            }
            return returnString;
        });

        // Helper to obtain the parameters and return strings of interop functions for public methods.
        handlebars.registerHelper("getInteropMethod", (object, options) -> {
            String returnString = "";
            if (object instanceof JMethod) {
                JMethod jMethod = (JMethod) object;
                String type = options.params[0].toString();
                if (type.equals("param")) {
                    returnString = getInteropMethodParam(jMethod);
                } else if (type.equals("return")) {
                    returnString = getInteropMethodReturn(jMethod);
                }
            }
            return returnString;
        });

        // Helper to obtain the parameters string of interop functions for public fields.
        handlebars.registerHelper("getInteropFieldParam", (object, options) -> {
            String returnString = "";
            if (object instanceof JField) {
                JField jField = (JField) object;
                returnString = getInteropFieldParam(jField);
            }
            return returnString;
        });

        try {
            return handlebars.compile(templateName);
        } catch (FileNotFoundException e) {
            throw new BindgenException("Code generation template file does not exist: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new BindgenException("Unable to read the " + templateName + " template file: " + e.getMessage(), e);
        }
    }

    private static String getInteropFieldParam(JField jField) {
        StringBuilder returnString = new StringBuilder();
        if (!jField.isStatic()) {
            returnString.append("handle receiver");
        }
        if (jField.isSetter()) {
            if (!jField.isStatic()) {
                returnString.append(", ");
            }
            returnString.append(jField.getExternalType()).append(" arg");
        }
        return returnString.toString();
    }

    private static String getInteropMethodReturn(JMethod jMethod) {
        StringBuilder returnString = new StringBuilder();
        if (jMethod.getHasReturn()) {
            returnString.append(" returns ").append(jMethod.getExternalType());
            if (jMethod.isHandleException()) {
                returnString.append("|error");
            }
        } else {
            if (jMethod.isHandleException()) {
                returnString.append(" returns error?");
            }
        }
        return returnString.toString();
    }

    private static String getInteropMethodParam(JMethod jMethod) {
        StringBuilder returnString = new StringBuilder();
        if (!jMethod.isStatic()) {
            returnString.append("handle receiver");
            if (jMethod.hasParams()) {
                returnString.append(", ");
            }
        }
        for (JParameter param: jMethod.getParameters()) {
            returnString.append(param.getExternalType()).append(" ").append(param.getFieldName());
            if (param.getHasNext()) {
                returnString.append(", ");
            }
        }
        return returnString.toString();
    }

    private static String getParamsHelper(JParameter param) {
        StringBuilder returnString = new StringBuilder();
        if (param.getIsObjArray()) {
            returnString.append("check getHandleFromObjectArray(").append(param.getFieldName())
                    .append(", \"").append(param.getComponentType()).append("\")");
        } else if (param.getIsPrimitiveArray()) {
            returnString.append("check getHandleFromArray(").append(param.getFieldName())
                    .append(", \"").append(param.getComponentType()).append("\")");
        } else if (param.getIsString()) {
            returnString.append("java:fromString(").append(param.getFieldName()).append(")");
        } else if (param.getIsStringArray()) {
            returnString.append("check getHandleFromArray(").append(param.getFieldName())
                    .append(", \"java.lang.String\")");
        } else {
            returnString.append(param.getFieldName());
            if (param.getIsObj()) {
                returnString.append(".jObj");
            }
        }
        if (param.getHasNext()) {
            returnString.append(", ");
        }
        return returnString.toString();
    }

    private static String getReturnHelper(JMethod jMethod) {
        StringBuilder returnString = new StringBuilder();
        if (jMethod.getHasReturn()) {
            returnString.append("returns ");
            returnString.append(jMethod.getReturnType());
            if (jMethod.getIsStringReturn()) {
                returnString.append("?");
            }
            if (jMethod.getHasException()) {
                returnString.append("|error");
            }
            returnString.append(" ");
        } else if (jMethod.getHasException() || jMethod.getHasPrimitiveParam()) {
            returnString.append("returns error? ");
        }
        return returnString.toString();
    }

    private static void listAllFiles(String directoryName, List<File> files) {
        File directory = new File(directoryName);
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    listAllFiles(file.getAbsolutePath(), files);
                }
            }
        }
    }

    public static void notifyExistingDependencies(Set<String> classList, File dependencyPath) {
        if (dependencyPath.isDirectory()) {
            List<File> listOfFiles = new ArrayList<>();
            listAllFiles(dependencyPath.toString(), listOfFiles);
            if (listOfFiles.size() > 1) {
                for (String className : classList) {
                    for (File file : listOfFiles) {
                        String fileName = getDependencyFileName(className);
                        if (file.getName().equals(fileName)) {
                            try {
                                Files.delete(file.toPath());
                                outStream.println("\nSuccessfully deleted the existing dependency: " + file.getPath());
                            } catch (IOException e) {
                                errStream.println("\nFailed to delete the existing dependency: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<String> getExistingBindings(Set<String> classList, File bindingsPath) {
        List<String> removeList = new ArrayList<>();
        if (bindingsPath.isDirectory()) {
            List<File> listOfFiles = new ArrayList<>();
            listAllFiles(bindingsPath.toString(), listOfFiles);
            if (listOfFiles.size() > 1) {
                for (String className : classList) {
                    for (File file : listOfFiles) {
                        String fileName = getDependencyFileName(className);
                        if (file.getName().equals(fileName)) {
                            removeList.add(className);
                            break;
                        }
                    }
                }
            }
        }
        return removeList;
    }

    private static String getDependencyFileName(String className) {
        char prefix;
        if (className.contains("$")) {
            prefix = '$';
        } else {
            prefix = '.';
        }
        return className.substring(className.lastIndexOf(prefix) + 1) + BAL_EXTENSION;
    }

    public static Set<String> getUpdatedConstantsList(Path existingPath, Set<String> classList) {
        try {
            List<String> allLines = Files.readAllLines(Paths.get(existingPath.toString()));
            List<String> removeList = new ArrayList<>();
            for (String className : classList) {
                for (String line : allLines) {
                    if (line.contains("\"" + className + "\"")) {
                        removeList.add(className);
                        break;
                    }
                }
            }
            classList.removeAll(removeList);
        } catch (IOException | ConcurrentModificationException e) {
            errStream.println("\nError while reading the existing constants file: " + e);
        }
        return classList;
    }

    public static void createDirectory(String path) throws BindgenException {
        File directory = new File(path);
        if (!directory.exists()) {
            try {
                final boolean mkdirResult = directory.mkdirs();
                if (!mkdirResult) {
                    throw new BindgenException("Unable to create the directory: " + path);
                }
            } catch (SecurityException e) {
                throw new BindgenException("Unable to create the directory: " + path, e);
            }
        }
    }

    public static Set<String> getClassNamesInJar(String jarPath) throws IOException {
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarPath));
        JarEntry jarEntry;
        Set<String> classes = new HashSet<>();
        try {
            while (true) {
                jarEntry = jarInputStream.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                if ((jarEntry.getName().endsWith(".class"))) {
                    String className = jarEntry.getName().replace("/", ".");
                    classes.add(className.substring(0, className.length() - ".class".length()));
                }
            }
            jarInputStream.close();
        } catch (IOException e) {
            throw new IOException("error while accessing the jar: ", e);
        } finally {
            jarInputStream.close();
        }
        return classes;
    }

    public static boolean isPublicField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers);
    }

    public static boolean isPublicMethod(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isPublic(modifiers);
    }

    public static boolean isPublicClass(Class javaClass) {
        int modifiers = javaClass.getModifiers();
        return Modifier.isPublic(modifiers);
    }

    public static boolean isStaticField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers);
    }

    public static boolean isStaticMethod(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isStatic(modifiers);
    }

    public static boolean isFinalField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isFinal(modifiers);
    }

    public static boolean isAbstractClass(Class javaClass) {
        int modifiers = javaClass.getModifiers();
        return Modifier.isAbstract(modifiers) && !javaClass.isInterface();
    }

    public static String getModuleName(String jarPath) {
        String name = null;
        if (jarPath != null) {
            Path jarName = Paths.get(jarPath).getFileName();
            if (jarName != null) {
                name = jarName.toString().substring(0, jarName.toString()
                        .lastIndexOf('.'));
                name = name.replace('-', '_');
            }
        }
        return name;
    }

    public static void handleOverloadedMethods(List<JMethod> methodList) {
        Map<String, Integer> methodNames = new HashMap<>();
        for (JMethod method : methodList) {
            String mName = method.getMethodName();
            if (methodNames.containsKey(mName)) {
                methodNames.replace(mName, methodNames.get(mName) + 1);
            } else {
                methodNames.put(mName, 1);
            }
        }
        for (Map.Entry<String, Integer> entry : methodNames.entrySet()) {
            if (entry.getValue() > 1) {
                int i = 1;
                for (JMethod jMethod : methodList) {
                    if (jMethod.getMethodName().equals(entry.getKey())) {
                        jMethod.setMethodName(jMethod.getMethodName() + i);
                        jMethod.setIsOverloaded(true);
                        i++;
                    }
                }
            }
        }
    }

    public static String getBallerinaParamType(Class javaType) {
        if (javaType.isArray() && javaType.getComponentType().isPrimitive()) {
            return getPrimitiveArrayBalType(javaType.getComponentType().getSimpleName());
        } else {
            String returnType = getBalType(javaType.getSimpleName());
            if (returnType.equals(HANDLE)) {
                return javaType.getSimpleName();
            }
            return returnType;
        }
    }

    public static String getBallerinaHandleType(Class javaType) {
        String type = javaType.getSimpleName();
        String returnType = getBalType(type);
        if (type.equals(JAVA_STRING) || type.equals(JAVA_STRING_ARRAY)) {
            returnType = HANDLE;
        }
        return returnType;
    }

    private static String getBalType(String type) {
        switch (type) {
            case INT:
                return INT;
            case FLOAT:
                return FLOAT;
            case BOOLEAN:
                return BOOLEAN;
            case BYTE:
                return BYTE;
            case SHORT:
                return INT;
            case CHAR:
                return INT;
            case DOUBLE:
                return FLOAT;
            case LONG:
                return INT;
            case JAVA_STRING:
                return BALLERINA_STRING;
            case JAVA_STRING_ARRAY:
                return BALLERINA_STRING_ARRAY;
            default:
                return HANDLE;
        }
    }

    private static String getPrimitiveArrayBalType(String type) {
        switch (type) {
            case INT:
                return INT_ARRAY;
            case FLOAT:
                return FLOAT_ARRAY;
            case BOOLEAN:
                return BOOLEAN_ARRAY;
            case BYTE:
                return BYTE_ARRAY;
            case SHORT:
                return INT_ARRAY;
            case CHAR:
                return INT_ARRAY;
            case DOUBLE:
                return FLOAT_ARRAY;
            case LONG:
                return INT_ARRAY;
            default:
                return HANDLE;
        }
    }

    public static String getPrimitiveArrayType(String type) {
        switch (type) {
            case "[C":
            case "[S":
            case "[J":
                return INT_ARRAY;
            case "[D":
                return FLOAT_ARRAY;
            case "[B":
                return BYTE_ARRAY;
            case "[I":
                return INT_ARRAY;
            case "[F":
                return FLOAT_ARRAY;
            case "[Z":
                return BOOLEAN_ARRAY;
            default:
                return type;
        }
    }

    public static URLClassLoader getClassLoader(Set<String> jarPaths, ClassLoader parent) throws BindgenException {
        URLClassLoader classLoader;
        List<URL> urls = new ArrayList<>();
        try {
            List<String> classPaths = new ArrayList<>();
            for (String path : jarPaths) {
                File file = FileSystems.getDefault().getPath(path).toFile();
                if (file.isDirectory()) {
                    File[] paths = file.listFiles();
                    if (paths != null) {
                        for (File filePath: paths) {
                            if (isJarFile(filePath)) {
                                urls.add(filePath.toURI().toURL());
                                classPaths.add(filePath.getName());
                            }
                        }
                    }
                } else {
                    if (isJarFile(file)) {
                        urls.add(file.toURI().toURL());
                        classPaths.add(file.getName());
                    }
                }
            }
            if (!classPaths.isEmpty()) {
                outStream.println("Following jars were added to the classpath:");
                for (String path : classPaths) {
                    outStream.println("\t" + path);
                }
            } else {
                errStream.println("Failed to add the provided jars to classpath.");
            }
            classLoader = (URLClassLoader) AccessController.doPrivileged((PrivilegedAction) ()
                    -> new URLClassLoader(urls.toArray(new URL[urls.size()]), parent));
        } catch (Exception e) {
            throw new BindgenException("Error while processing the classpaths.", e);
        }
        return classLoader;
    }

    private static boolean isJarFile(File file) {
        String fileName = file.getName();
        if (file.isFile() && fileName.substring(fileName.lastIndexOf('.')).equals(".jar")) {
            return true;
        }
        return false;
    }
}
