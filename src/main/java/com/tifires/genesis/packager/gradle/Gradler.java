package com.tifires.genesis.packager.gradle;

import com.tifires.genesis.packager.commons.Resource;
import com.tifires.genesis.packager.commons.SourceCode;
import com.tifires.genesis.stuffs.Utils;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;

public class Gradler {

    private final static Logger LOG = LoggerFactory.getLogger(Gradler.class);
    private final JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/build.gradle.twig");
    private static final String srcPath = "src/main/java";
    private static final String resPath = "src/main/resources";
    private Path projectLocation;
    private File src;
    private File res;
    private File projectDirectory;

    public static Gradler newInstance() {
        return new Gradler();
    }

    public void setLocation(Path projectLocation) {
        this.projectLocation = projectLocation;
    }

    public Gradler init() {
        projectDirectory = projectLocation.toFile();
        if (!projectDirectory.exists())
            projectDirectory.mkdirs();

        try {
            FileOutputStream build = new FileOutputStream(Paths.get(projectLocation.toString(), "build.gradle").toFile());
            build.write(makeBuildGradle().getBytes());
            build.close();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        src = Paths.get(projectLocation.toString(), "src/main/java/").toFile();
        if (!src.exists())
            src.mkdirs();

        res = Paths.get(projectLocation.toString(), "src/main/resources/").toFile();
        if (!res.exists())
            res.mkdirs();

        return this;
    }

    public Gradler addSource(String packname, String classname, String srccontent) {
        File pack = Paths.get(src.getAbsolutePath(), packname.replaceAll("\\.", "/")).toFile();
        if (!pack.exists())
            pack.mkdirs();
        try {
            FileOutputStream srcFile = new FileOutputStream(Paths.get(pack.toString(), classname + ".java").toFile());
            srcFile.write(srccontent.getBytes());
            srcFile.close();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return this;
    }


    public Gradler pack() {
        GradleConnector gradleConnector = GradleConnector.newConnector();
        ((DefaultGradleConnector) gradleConnector).embedded(true);
        ProjectConnection conn = gradleConnector.forProjectDirectory(projectDirectory).connect();
        BuildLauncher launcher = conn.newBuild();
        launcher.setStandardOutput(System.out);
        launcher.setStandardError(System.err);
        launcher.forTasks("init", "clean", "build").run();
        conn.close();
        return this;
    }

    private String makeBuildGradle() {
        JtwigModel model = JtwigModel.newModel();
        return template.render(model);
    }

    public void pushResources(String location, Set<Resource> resources) {
        Objects.requireNonNull(resources);
        resources.forEach(resource -> pushResource(location, resource));
    }

    public void pushResource(String location, Resource resource) {
        File resPath = Paths.get(res.getAbsolutePath(), location.replaceAll("\\.", "/")).toFile();
        if (!resPath.exists())
            resPath.mkdirs();

        try {
            FileOutputStream resFile = new FileOutputStream(Paths.get(resPath.toString(), resource.getFilename()).toFile());
            resFile.write(resource.getContent());
            resFile.close();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void pushSource(SourceCode sourceCode) {
        String packagename = sourceCode.getClassName().substring(0, sourceCode.getClassName().lastIndexOf('.'));
        String classname = Utils.capFirst(sourceCode.getClassName().substring(sourceCode.getClassName().lastIndexOf('.') + 1));
        addSource(packagename, classname, sourceCode.getContent());
    }
}
