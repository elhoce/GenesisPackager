package com.tifires.genesis.packager.pack;

import com.tifires.genesis.packager.commons.CompiledCode;
import com.tifires.genesis.packager.commons.Resource;
import com.tifires.genesis.packager.commons.SourceCode;
import com.tifires.genesis.packager.compile.Compiler;
import com.tifires.genesis.packager.gradle.Gradler;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Packager {
    private final static Logger LOG = LoggerFactory.getLogger(Packager.class);
    private Path pjLoc;
    private Path name;
    private Compiler compiler;
    private Map<String, Set<Resource>> mResources = new HashMap<>();
    private String version = "1.0.0";
    private String author = "tifires.com";
    private ObjectMapper mapper = new ObjectMapper();
    private JarOutputStream jos = null;


    public static Packager newInstance() {
        return new Packager();
    }

    public Packager() {
        try {
            pjLoc = Files.createTempDirectory("genesis");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        compiler = Compiler.newInstance();
    }

    public Packager setFilename(String filename) {
        return setLocation(null, filename);
    }

    public Packager setLocation(Path location, String filename) {
        if (location != null)
            pjLoc = location;
        if (!filename.endsWith(".jar"))
            filename = filename.concat(".jar");
        try {
            name = Files.createFile(Paths.get(pjLoc.toString(), filename));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return this;
    }

    public Packager addSource(String classname, String content) {
        try {
            compiler.addSource(classname, content);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return this;
    }

    public Packager addResources(String location, Resource... json) {
        Set<Resource> files;

        if (mResources.containsKey(location)) {
            files = mResources.get(location);
        } else {
            files = new HashSet<>();
            mResources.put(location, files);
        }

        for (Resource resource : json) {
            files.add(resource);
        }

        return this;
    }

    public Packager pack(boolean withSources, boolean useGradle) {
        return useGradle ? this.packWithGradle(withSources) : this.packInMemory(withSources);
    }

    private Packager packWithGradle(boolean withSources) {
        Gradler gradler = Gradler.newInstance();
        gradler.setLocation(pjLoc);
        gradler.init();
        mResources.forEach(gradler::pushResources);
        compiler.getSources().forEach(gradler::pushSource);
        gradler.pack();
        name = Paths.get(pjLoc.toString(),"build","libs",pjLoc.getFileName().toString().concat(".jar"));
        return this;
    }

    private Packager packInMemory(boolean withSources) {
        //prepare Manifest file
        Manifest manifest = new Manifest();
        Attributes global = manifest.getMainAttributes();
        global.put(Attributes.Name.MANIFEST_VERSION, version);
        global.put(new Attributes.Name("Created-By"), author);

        //create required jar name
        try {
            name = Files.createTempFile(pjLoc, "pack", ".jar");
            File jarFile = name.toFile();
            OutputStream os = new FileOutputStream(jarFile);
            jos = new JarOutputStream(os, manifest);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }

        //resources
        mResources.forEach(this::pushResources);

        //compile
        compiler.compileAll();

        //get sources & compiled sources
        if (withSources)
            compiler.getResources().forEach(this::pushCompiledSrcWthSrc);
        else
            compiler.getResources().forEach(this::pushCompiledSrc);

        //close archive
        try {
            jos.close();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return this;
    }

    private void pushCompiledSrc(SourceCode sourceCode, CompiledCode compiledCode) {
        Objects.requireNonNull(compiledCode);

        try {
            Class<?> tClass = compiler.getClassForSrc(compiledCode);
            String packagename = tClass.getName().substring(0, tClass.getName().lastIndexOf('.'));
            String filename = tClass.getName().substring(tClass.getName().lastIndexOf('.') + 1).concat(".class");
            Resource resource = new Resource(filename, compiledCode.getByteCode());
            pushResource(packagename, resource);
        } catch (ClassNotFoundException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void pushCompiledSrcWthSrc(SourceCode sourceCode, CompiledCode compiledCode) {
        Objects.requireNonNull(sourceCode);
        pushCompiledSrc(sourceCode, compiledCode);
        try {
            Class<?> tClass = compiler.getClassForSrc(compiledCode);
            String packagename = tClass.getName().substring(0, tClass.getName().lastIndexOf('.'));
            String filename = tClass.getName().substring(tClass.getName().lastIndexOf('.') + 1).concat(".java");
            Resource resource = new Resource(filename, sourceCode.getContentAsBytes());
            pushResource(packagename, resource);
        } catch (ClassNotFoundException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void pushResources(String location, Set<Resource> resources) {
        Objects.requireNonNull(resources);
        resources.forEach(resource -> pushResource(location, resource));
    }

    private void pushResource(String location, Resource resource) {
        final String path = location.replaceAll("\\.", "/");
        JarEntry entry = new JarEntry(String.join("/", path, resource.getFilename()));
        try {
            jos.putNextEntry(entry);
            jos.write(resource.getContent());
            jos.closeEntry();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Path getLocation() {
        return name;
    }
}
