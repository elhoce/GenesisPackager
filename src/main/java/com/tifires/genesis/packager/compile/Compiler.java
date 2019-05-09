package com.tifires.genesis.packager.compile;

import com.tifires.genesis.packager.commons.CompiledCode;
import com.tifires.genesis.packager.commons.SourceCode;

import javax.tools.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compile Java sources in-memory
 */
public class Compiler {
    private JavaCompiler javac;
    private DynamicClassLoader classLoader;
    private Iterable<String> options;
    private boolean ignoreWarnings = false;

    private Map<String, SourceCode> sourceCodes = new HashMap<>();

    public static Compiler newInstance() {
        return new Compiler();
    }

    private Compiler() {
        this.javac = ToolProvider.getSystemJavaCompiler();
        this.classLoader = new DynamicClassLoader(ClassLoader.getSystemClassLoader());
    }

    public Compiler useParentClassLoader(ClassLoader parent) {
        this.classLoader = new DynamicClassLoader(parent);
        return this;
    }

    /**
     * @return the class loader used internally by the compiler
     */
    public ClassLoader getClassloader() {
        return classLoader;
    }

    /**
     * Options used by the compiler, e.g. '-Xlint:unchecked'.
     *
     * @param options
     * @return
     */
    public Compiler useOptions(String... options) {
        this.options = Arrays.asList(options);
        return this;
    }

    /**
     * Ignore non-critical compiler output, like unchecked/unsafe operation
     * warnings.
     *
     * @return
     */
    public Compiler ignoreWarnings() {
        ignoreWarnings = true;
        return this;
    }

    /**
     * Compile all sources
     *
     * @return Map containing instances of all compiled classes
     */
    public Map<String, Class<?>> compileAll() {
        if (sourceCodes.size() == 0) {
            throw new CompilationException("No source code to compile");
        }
        Collection<SourceCode> compilationUnits = sourceCodes.values();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        ExtendedStandardJavaFileManager fileManager = new ExtendedStandardJavaFileManager(javac.getStandardFileManager(null, null, null), classLoader);
        JavaCompiler.CompilationTask task = javac.getTask(null, fileManager, collector, options, null, compilationUnits);
        boolean result = task.call();
        if (!result || collector.getDiagnostics().size() > 0) {
            StringBuilder exceptionMsg = new StringBuilder("Unable to compile the source");
            boolean hasWarnings = false;
            boolean hasErrors = false;
            for (Diagnostic<? extends JavaFileObject> d : collector.getDiagnostics()) {
                switch (d.getKind()) {
                    case NOTE:
                    case MANDATORY_WARNING:
                    case WARNING:
                        hasWarnings = true;
                        break;
                    case OTHER:
                    case ERROR:
                    default:
                        hasErrors = true;
                        break;
                }
                exceptionMsg.append("\n").append("[kind=").append(d.getKind());
                exceptionMsg.append(", ").append("line=").append(d.getLineNumber());
                exceptionMsg.append(", ").append("message=").append(d.getMessage(Locale.US)).append("]");
            }
            if (hasWarnings && !ignoreWarnings || hasErrors) {
                throw new CompilationException(exceptionMsg.toString());
            }
        }

        return sourceCodes.keySet().stream().collect(Collectors.toMap(classname -> classname, classname -> {
            try {
                return classLoader.loadClass(classname);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }));
    }

    /**
     * Compile single source
     *
     * @param className
     * @param sourceCode
     * @return
     * @throws Exception
     */
    public Class<?> compile(String className, String sourceCode) throws Exception {
        return addSource(className, sourceCode).compileAll().get(className);
    }

    /**
     * Add source code to the compiler
     *
     * @param className
     * @param sourceCode
     * @return
     * @throws Exception
     * @see {@link #compileAll()}
     */
    public Compiler addSource(String className, String sourceCode) throws Exception {
        sourceCodes.put(className, new SourceCode(className, sourceCode));
        return this;
    }

    public Map<SourceCode, CompiledCode> getResources() {
        Map<String, CompiledCode> compiledCodes = classLoader.getCompiledCodes();
        return compiledCodes.keySet().stream().collect(Collectors.toMap(sourceCodes::get, compiledCodes::get));
    }

    public List<SourceCode> getSources() {
        return new ArrayList<>(sourceCodes.values());
    }

    public Class<?> getClassForSrc(CompiledCode resource) throws ClassNotFoundException {
        Objects.requireNonNull(resource);
        return classLoader.loadClass(resource.getClassName());
    }
}
