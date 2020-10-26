package com.arvato.workflow.kie4developer.common.impl;

import com.arvato.workflow.kie4developer.common.interfaces.IDeployableDependency;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Helper to fetch Files from Filesystem
 *
 * @author TRIBE01
 */
@Component
public class FileSystemUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemUtils.class);
  private ApplicationContext applicationContext;
  private Path mavenRepository;

  public FileSystemUtils(ApplicationContext applicationContext, @Value("${maven.repository}") String mavenRepository){
    this.applicationContext = applicationContext;
    this.mavenRepository = Paths.get(mavenRepository);
  }

  /**
   * Get a file from the project dir
   *
   * @param path the path relative from the project root dir to fetch
   * @return the found File or <code>null</code>
   */
  public File getFile(Path path){
    Path basePath;
    if (runAsFatJar()) {
      basePath = unzip(getFatJarFile()).toPath();
    }else{
      basePath = Paths.get("");
    }
    return basePath.resolve(path).toFile();
  }

  /**
   * Get the unzipped jar file for a dependency
   *
   * @param dependency the dependency to fetch
   * @return the found File or <code>null</code>
   */
  public File getUnzippedMavenDependencyJarFile(IDeployableDependency dependency){
    return unzip(getMavenDependencyJarFile(dependency));
  }

  /**
   * Get the jar file for a dependency
   *
   * @param dependency the dependency to fetch
   * @return the found File or <code>null</code>
   */
  private File getMavenDependencyJarFile(IDeployableDependency dependency){
    if (runAsFatJar()) {
      // running as fat jar... fetch dependencies from within fat jar
      return getFile(Paths.get("BOOT-INF", "lib", dependency.getMavenArtifactId() + "-" + dependency.getMavenVersionId() + ".jar"));
    }else{
      // running via IDE... fetch dependencies from maven repo
      return new File(mavenRepository.toFile().getAbsolutePath() + File.separator + dependency.getMavenGroupId().replace(".", File.separator) + File.separator + dependency.getMavenArtifactId() + File.separator + dependency.getMavenVersionId() + File.separator + dependency.getMavenArtifactId() + "-" + dependency.getMavenVersionId() + ".jar");
    }
  }

  /**
   * Check if the application runs as fat-jar
   *
   * @return <code>true</code> if running as fat-jar, <code>false</code> if running in IDE
   */
  public boolean runAsFatJar() {
    Class mainClassOnProject = applicationContext.getBeansWithAnnotation(SpringBootApplication.class).values().toArray()[0].getClass();
    String compiledClassesDir = mainClassOnProject.getProtectionDomain().getCodeSource().getLocation().getFile();
    return compiledClassesDir.contains(".jar");
  }

  /**
   * Get the fat-jar as file
   *
   * @return the fat-jar file or <code>null</code>
   */
  private File getFatJarFile() {
    if (!runAsFatJar()){
      return null;
    }
    String compiledClassesDir = FileSystemUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    compiledClassesDir = compiledClassesDir.startsWith("file:") ? compiledClassesDir.substring(5) : compiledClassesDir;
    compiledClassesDir = compiledClassesDir.contains("!") ? compiledClassesDir.substring(0, compiledClassesDir.indexOf("!")) : compiledClassesDir;
    try {
      compiledClassesDir = URLDecoder.decode(compiledClassesDir, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Error while decoding dir: {}", compiledClassesDir, e);
      return null;
    }
    return new File(compiledClassesDir);
  }

  /**
   * Unzip a zip/jar to a tmp directory
   *
   * @param zipFile the jar or zip file to extract
   * @return the temp directory which contains all extracted folders and files of the archive
   */
  public File unzip(File zipFile) {
    Path outputPath = null;
    try {
      outputPath = Files.createTempDirectory(UUID.randomUUID().toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
    try (ZipFile zf = new ZipFile(zipFile)) {
      Enumeration<? extends ZipEntry> zipEntries = zf.entries();
      while (zipEntries.hasMoreElements()) {
        ZipEntry entry = zipEntries.nextElement();
        if (entry.isDirectory()) {
          Path dirToCreate = outputPath.resolve(entry.getName());
          Files.createDirectories(dirToCreate);
        } else {
          Path fileToCreate = outputPath.resolve(entry.getName());
          fileToCreate.toFile().getParentFile().mkdirs();
          Files.copy(zf.getInputStream(entry), fileToCreate);
        }
      }
    } catch (IOException e) {
      LOGGER.error("Error while unzipping: {}", zipFile, e);
      return null;
    }
    return outputPath.toFile();
  }

  /**
   * Delete all files within and the directory itself
   *
   * @param path filesystem path to delete
   * @throws IOException on any I/O error
   */
  public void delete(Path path) throws IOException {
    if (path == null || !Files.exists(path)) {
      return;
    }
    if (Files.isDirectory(path)) {
      Stream<Path> children = Files.list(path);
      children.forEach(child -> {
        try {
          delete(child);
        } catch (IOException e) {
          LOGGER.error("Error while deleting file {}", child, e);
        }
      });
    }
    Files.delete(path);
  }
}
