/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jmonitor.installer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.io.FilenameUtils;
import org.jmonitor.installer.TempDirectoryUtils.TempDirectoryCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main shaded installer entry point.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO major copy paste here from InstallerMain, need to consolidate
public class InstallerMain {

    private static final String EXCLUDE_DEFAULT_PROBES_ARG_NAME = "excludeDefaultProbes";
    private static final String UNSHADED_ARG_NAME = "unshaded";
    private static final String KEEP_STAGING_DIR_ARG_NAME = "keepStagingDir";

    private static final String DEFAULT_PROBES_JAR_RESOURCE_NAME = "jmonitor-probes.jar";
    private static final String RUNTIME_JAR_RESOURCE_NAME = "jmonitor-runtime.jar";
    private static final String INSTALLER_BASE_JAR_RESOURCE_NAME = "jmonitor-installer-base.jar";

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallerMain.class);

    private File inputFile;
    private File outputFile;
    private List<File> probeJarFiles;
    private List<String> ajcArgs;
    private boolean excludeDefaultProbes;
    private boolean unshaded;
    private boolean keepStagingDir;

    // utility class
    private InstallerMain() {
    }

    public static void main(String[] args) throws IOException {
        new InstallerMain().runMain(args, true);
    }

    public void runMain(String[] args, boolean useSystemExit) throws IOException {

        OptionParser parser = new OptionParser();
        OptionSpec<String> ajcArgsOptionSpec =
                parser.accepts("A").withRequiredArg().describedAs("ajc arg").ofType(String.class);
        OptionSpec<File> probeJarFilesOptionSpec =
                parser.accepts("P").withRequiredArg().describedAs("probe jar").ofType(File.class);
        OptionSpec<File> outputFileOptionSpec =
                parser.accepts("out", "woven output file").withOptionalArg().ofType(File.class);
        parser.accepts(EXCLUDE_DEFAULT_PROBES_ARG_NAME, "do not weave default jmonitor probes");
        parser.accepts(UNSHADED_ARG_NAME, "do not shade jmonitor dependencies "
                + "(using this option could lead to library version conflicts with the woven app)");
        parser.accepts(KEEP_STAGING_DIR_ARG_NAME,
                "do not delete temp files used during installation (for debugging)");
        parser.acceptsAll(Arrays.asList("?", "h"), "show help");

        try {
            OptionSet options = parser.parse(args);

            if (options.has("?")) {
                System.out.println("Usage: java " + InstallerMain.class.getName() // NOPMD for
                        // System.out.print
                        // usage
                        + " <options> <input ear/war file>");
                parser.printHelpOn(System.out);
                return;
            }

            ajcArgs = options.valuesOf(ajcArgsOptionSpec);
            probeJarFiles = options.valuesOf(probeJarFilesOptionSpec);
            outputFile = options.valueOf(outputFileOptionSpec);

            List<String> remaining = options.nonOptionArguments();
            if (remaining.size() != 1) {
                throw new MiscOptionException("there must be exactly one ear/war");
            }

            String inputFilename = remaining.get(0);
            inputFile = new File(inputFilename);

            validateInputFile(inputFile);
            validateProbeJarFiles(probeJarFiles);

            if (outputFile == null) {
                String baseName = FilenameUtils.getBaseName(inputFilename);
                String extension = FilenameUtils.getExtension(inputFilename);
                outputFile = new File(baseName + "-woven." + extension);
            }

            excludeDefaultProbes = options.has(EXCLUDE_DEFAULT_PROBES_ARG_NAME);
            unshaded = options.has(UNSHADED_ARG_NAME);
            keepStagingDir = options.has(KEEP_STAGING_DIR_ARG_NAME);

        } catch (OptionException e) {

            LOGGER.error(e.getMessage());
            if (useSystemExit) {
                System.exit(1); // NOPMD for using System.exit()
            }
            return;
        }

        validateOutputFile(outputFile);

        TempDirectoryUtils.execute("jmonitor-installer-", !keepStagingDir,
                new TempDirectoryCallback() {
                    public void doWithTempDirectory(File tempDirectory) throws IOException {

                        File tempExtractDirectory = new File(tempDirectory, "extract");
                        tempExtractDirectory.mkdir();
                        
                        // extract jmonitor-installer.jar from jmonitor-shaded-installer.jar
                        File installerJarFile =
                                ResourceUtils.extractResource(INSTALLER_BASE_JAR_RESOURCE_NAME,
                                        tempExtractDirectory);

                        // extract jmonitor-runtime.jar from jmonitor-installer.jar
                        File runtimeJarFile =
                            ResourceUtils.extractResource(RUNTIME_JAR_RESOURCE_NAME,
                                    tempExtractDirectory);

                        if (!excludeDefaultProbes) {
                            // extract jmonitor-probes.jar from jmonitor-installer.jar
                            File defaultProbesJarFile =
                                ResourceUtils.extractResource(DEFAULT_PROBES_JAR_RESOURCE_NAME,
                                        tempExtractDirectory);
                            // add it to the beginning of the probes list
                            // but first we have to copy the list since it is immutable
                            probeJarFiles = new ArrayList<File>(probeJarFiles);
                            probeJarFiles.add(0, defaultProbesJarFile);
                        }

                        if (unshaded) {

                            runInstaller(installerJarFile, runtimeJarFile, probeJarFiles,
                                    tempDirectory);

                        } else {
                            
                            File tempShadedDirectory = new File(tempDirectory, "shaded");
                            tempShadedDirectory.mkdir();

                            // shade jmonitor-installer.jar
                            File shadedInstallerJarFile =
                                    Shader.shadeInstallerJar(installerJarFile, tempShadedDirectory);

                            // shade jmonitor-runtime.jar
                            File shadedRuntimeJarFile =
                                    Shader.shadeRuntimeJar(runtimeJarFile, tempShadedDirectory);

                            // shade probes jar files
                            List<File> shadedProbeJarFiles =
                                    Shader.shadeProbeJars(probeJarFiles, tempShadedDirectory);

                            runInstaller(shadedInstallerJarFile, shadedRuntimeJarFile,
                                    shadedProbeJarFiles, tempDirectory);
                        }
                    }
                });
    }

    private void runInstaller(File installerJarFile, File runtimeJarFile, List<File> probeJarFiles,
            File tempDirectory) throws MalformedURLException {

        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();

        // create a new class loader
        ClassLoader installerClassLoader =
                new URLClassLoader(new URL[] {installerJarFile.toURL()},
                        parentClassLoader);

        // set the context class loader
        Thread.currentThread().setContextClassLoader(installerClassLoader);

        try {

            Class<?> installerClass =
                    Class.forName("org.jmonitor.installer.base.Installer", true,
                            installerClassLoader);

            Constructor<?> installerConstructor =
                    installerClass.getConstructor(File.class, File.class, File.class, List.class,
                            List.class, File.class);
            Object installer =
                    installerConstructor.newInstance(inputFile, outputFile, runtimeJarFile,
                            probeJarFiles, ajcArgs, tempDirectory);
            installerClass.getMethod("install").invoke(installer);

        } catch (ClassNotFoundException e) {
            // indicates internal jmonitor error, re-throw exception
            throw new IllegalStateException(e);
        } catch (SecurityException e) {
            // indicates internal jmonitor error, re-throw exception
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            // indicates internal jmonitor error, re-throw exception
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            // indicates internal jmonitor error, re-throw exception
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            // indicates internal jmonitor error, re-throw exception
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            // indicates internal jmonitor error, re-throw exception
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            // indicates internal jmonitor error, re-throw exception
            throw new IllegalStateException(e);
        } finally {
            
            // finally, return to original context class loader
            Thread.currentThread().setContextClassLoader(parentClassLoader);
        }
    }

    private static void validateOutputFile(File outputFile) {
        if (outputFile.exists()) {
            throw new MiscOptionException("output file already exists: " + outputFile.getPath());
        }
    }

    private static void validateProbeJarFiles(List<File> probeJarFiles) {
        for (File probeJarFile : probeJarFiles) {
            if (!probeJarFile.getPath().endsWith(".jar")) {
                throw new MiscOptionException("probe filenames must end with .jar");
            } else if (!probeJarFile.exists()) {
                throw new MiscOptionException("cannot find probe jar file: "
                        + probeJarFile.getPath());
            }
        }
    }

    private static void validateInputFile(File inputFile) {
        if (!inputFile.getName().endsWith(".ear") && !inputFile.getName().endsWith(".war")) {
            throw new MiscOptionException("ear/war filename must end with .ear or .war");
        } else if (!inputFile.exists()) {
            throw new MiscOptionException("cannot find ear/war file: " + inputFile.getPath());
        }
    }

    private static class MiscOptionException extends OptionException {

        private static final long serialVersionUID = 1L;
        private static final Collection<String> EMPTY_OPTIONS = Collections.emptySet();

        private final String message;

        protected MiscOptionException(String message) {
            super(EMPTY_OPTIONS);
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
