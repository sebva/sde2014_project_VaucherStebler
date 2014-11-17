package ch.unibe.sde2014.project;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class CallGraphAgent implements ClassFileTransformer {

    private final ClassPool classPool;
    private Logger logger;

    // Blacklist and/or whitelist can be ignored by setting either to null
    private static final List<String> BLACKLIST = new ArrayList<>(Arrays.asList(new String[]{
            "java.",
            "javax.",
            "sun.",
            "com.sun",
            "ch.unibe.sde2014.project"
    }));
    private static final List<String> WHITELIST = null;

    public CallGraphAgent() {
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        classPool = new ClassPool();
        classPool.appendSystemPath();
        try {
            classPool.appendPathList(System.getProperty("java.class.path"));

            classPool.get("ch.unibe.sde2014.project.CallGraph").getClass();
            classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"UnusedDeclaration", "UnusedParameters"})
    public static void premain(String agentArguments, Instrumentation instrumentation) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    mainThread.join();
                    CallGraph.writeToFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        instrumentation.addTransformer(new CallGraphAgent());

        // No Logger usage, it is not yet ready.
        System.out.println("CallGraphAgent registered.");
    }

    @Override
    public synchronized byte[] transform(ClassLoader loader, String classNameWithSlashes, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classBytes) throws IllegalClassFormatException {
        String className = classNameWithSlashes.replace('/', '.');

        classPool.appendClassPath(new ByteArrayClassPath(className, classBytes));

        try {
            CtClass ctClass = classPool.get(className);
            if (ctClass.isFrozen()) {
                logger.warning(className + " is frozen");
                return null;
            }

            if (BLACKLIST != null) {
                for (String aPackage : BLACKLIST) {
                    if (ctClass.getPackageName().startsWith(aPackage)) {
                        return null;
                    }
                }
            }
            if (WHITELIST != null) {
                boolean found = false;
                for (String aPackage : WHITELIST) {
                    if (ctClass.getPackageName().startsWith(aPackage)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return null;
                }
            }

            if (ctClass.isPrimitive() || ctClass.isArray() || ctClass.isAnnotation() || ctClass.isEnum() || ctClass.isInterface()) {
                return null;
            }

            for (CtMethod method : ctClass.getDeclaredMethods()) {
                if (method.getMethodInfo().getCodeAttribute() == null) {
                    continue;
                }

                try {
                    method.addLocalVariable("__inTime", CtClass.longType);
                    method.insertBefore("{ ch.unibe.sde2014.project.CallGraph.getInstance(Thread.currentThread()).enterMethod(\"" + method.getLongName() + "\"); }");
                    method.insertAfter("{ ch.unibe.sde2014.project.CallGraph.getInstance(Thread.currentThread()).exitMethod(); }");
                } catch (Exception e) {
                    logger.warning("Error with method " + method.getLongName() + ": " + e.getMessage());
                }
            }
            classBytes = ctClass.toBytecode();
            ctClass.detach();
            return classBytes;
        } catch (Exception e) {
            logger.warning("Error with class " + className + ": " + e.getMessage());
        }
        return classBytes;
    }
}
