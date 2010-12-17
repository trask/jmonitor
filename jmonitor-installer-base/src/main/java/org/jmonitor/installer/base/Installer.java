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

package org.jmonitor.installer.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jmonitor.installer.base.util.FormatUtils;
import org.jmonitor.installer.base.util.ResourceUtils;
import org.jmonitor.installer.base.util.StreamUtils;
import org.jmonitor.installer.base.util.Weaver;
import org.jmonitor.installer.base.util.WebXml;
import org.jmonitor.installer.base.util.StreamUtils.JarStreamCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class Installer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Installer.class);

    // this is used so that war/ear files can be built for load time weaving, but can still later be
    // compile time woven
    // TODO generalize pattern to match deployed snapshot versions, e.g. "1.0-20100502.191338-3"
    private static final Pattern LTW_RUNTIME_JAR_RESOURCE_PATTERN =
            Pattern.compile("WEB-INF/lib/jmonitor-runtime-\\d+\\.\\d+(?:\\.\\d+)?(?:-SNAPSHOT)?.jar");
    private static final String RUNTIME_JAR_RESOURCE_NAME = "jmonitor-runtime.jar";
    private static final String JAVAEE_JAR_RESOURCE_NAME = "javaee-api.jar";

    private final File inputFile;
    private final File outputFile;
    private final File runtimeJarFile;
    private final List<File> probeJarFiles;
    private final List<String> ajcArgs;
    private final File tempDirectory;

    public Installer(File inputFile, File outputFile, File runtimeJarFile,
            List<File> probeJarFiles, List<String> ajcArgs, File tempDirectory) {

        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.runtimeJarFile = runtimeJarFile;
        this.probeJarFiles = probeJarFiles;
        this.ajcArgs = ajcArgs;
        this.tempDirectory = tempDirectory;
    }

    public void install() throws IOException, FileNotFoundException {

        long startMillis = System.currentTimeMillis();

        installInternal(tempDirectory);

        long endMillis = System.currentTimeMillis();
        Date endDate = new Date(endMillis);
        LOGGER.info("completed in "
                + FormatUtils.formatAsMinutesAndSeconds(endMillis - startMillis));
        LOGGER.info("completed at " + DateFormat.getDateTimeInstance().format(endDate));
    }

    private void installInternal(File tempDirectory) throws IOException, FileNotFoundException {

        File tempWovenFile = new File(tempDirectory, outputFile.getName());

        // extract Xlint.properties which is used during aspectj compilation
        File xlintPropertiesFile = ResourceUtils.extractResource("Xlint.properties", tempDirectory);

        // TODO merge Xlintfile options and Xset options
        List<String> mergedAjcArgs = new ArrayList<String>();
        // TODO doc why
        mergedAjcArgs.add("-Xlintfile");
        mergedAjcArgs.add(xlintPropertiesFile.getAbsolutePath());
        // TODO doc why
        mergedAjcArgs.add("-Xset:useWeakTypeRefs=false,typeDemotion=true");

        // STUFF MAYBE(??) AFTER ASPECTJ 1.6.10 UPGRADE
        // since we are shading aspectj we don't need to worry about reweaving
        // also since we are shading aspectj we don't need -Xset:overWeaving=true
        // mergedAjcArgs.add("-XnotReweavable");

        // minimalModel=true,typeDemotion=true helps to minimize memory footprint
        // targetRuntime1_6_10=true makes the woven code "a little shorter/neater"
        // for more details on these options see
        // http://www.eclipse.org/aspectj/doc/released/README-1610.html
        //
        // mergedAjcArgs.add("-Xset:minimalModel=true,typeDemotion=true,targetRuntime1_6_10=true");
        // TODO doc why
        mergedAjcArgs.add("-checkRuntimeVersion:false");
        mergedAjcArgs.addAll(ajcArgs);

        // extract javaee-api.jar which needs to be added to weaving classpath
        File javaeeJarFile = ResourceUtils.extractResource(JAVAEE_JAR_RESOURCE_NAME, tempDirectory);
        List<File> aspectClasspath = Arrays.asList(runtimeJarFile, javaeeJarFile);

        File weaverTempDirectory = new File(tempDirectory, "weaver");
        final Weaver weaver =
                new Weaver(probeJarFiles, aspectClasspath, mergedAjcArgs,
                        Collections.singletonList(LTW_RUNTIME_JAR_RESOURCE_PATTERN),
                        weaverTempDirectory);

        if (inputFile.getName().endsWith(".ear")) {

            LOGGER.info("weaving ear file " + inputFile.getName() + " ..");

            // weave the ear file
            StreamUtils.execute(inputFile, tempWovenFile, new JarStreamCallback() {
                public void doWithJarStreams(JarInputStream earInputStream,
                        JarOutputStream earOutputStream) throws IOException {

                    weaver.weaveEar(earInputStream, earOutputStream);
                }
            });

            LOGGER.info("post-processing ear file " + inputFile.getName() + " ..");

            // post-process the ear file (add jmonitor.jar and modify web.xml)
            StreamUtils.execute(tempWovenFile, outputFile, new JarStreamCallback() {
                public void doWithJarStreams(JarInputStream earInputStream,
                        JarOutputStream earOutputStream) throws IOException {

                    addMonitorToEar(earInputStream, earOutputStream, runtimeJarFile, probeJarFiles);
                }
            });

        } else if (inputFile.getName().endsWith(".war")) {

            LOGGER.info("weaving war file " + inputFile.getName() + " ..");

            // weave the war file
            StreamUtils.execute(inputFile, tempWovenFile, new JarStreamCallback() {
                public void doWithJarStreams(JarInputStream warInputStream,
                        JarOutputStream warOutputStream) throws IOException {

                    weaver.weaveWar(warInputStream, warOutputStream);
                }
            });

            LOGGER.info("post-processing war file " + inputFile.getName() + " ..");

            // post-process the war file (add jmonitor.jar and modify web.xml)
            StreamUtils.execute(tempWovenFile, outputFile, new JarStreamCallback() {
                public void doWithJarStreams(JarInputStream warInputStream,
                        JarOutputStream warOutputStream) throws IOException {

                    addMonitorToWar(warInputStream, warOutputStream, runtimeJarFile, probeJarFiles);
                }
            });

        } else {

            throw new IllegalStateException("ear/war filename must end with .ear or .war");
        }
    }

    private static void addMonitorToEar(JarInputStream earInputStream,
            JarOutputStream earOutputStream, File shadedRuntimeJarFile,
            List<File> shadedProbeJarFiles) throws IOException {

        JarEntry earEntry;
        while ((earEntry = earInputStream.getNextJarEntry()) != null) {
            if (earEntry.getName().matches("[^/]*\\.war")) {

                LOGGER.info("post-processing war file " + earEntry.getName() + " ..");

                // found a war file
                JarEntry newJarEntry = new JarEntry(earEntry.getName());
                earOutputStream.putNextEntry(newJarEntry);
                // add monitor to the war file
                JarInputStream warInputStream = new JarInputStream(earInputStream);
                JarOutputStream warOutputStream = new JarOutputStream(earOutputStream);
                addMonitorToWar(warInputStream, warOutputStream, shadedRuntimeJarFile,
                        shadedProbeJarFiles);
                // we cannot close it since that would close the underlying stream
                // so instead we "finish"
                warOutputStream.finish();
            } else {
                JarEntry newJarEntry = new JarEntry(earEntry.getName());
                earOutputStream.putNextEntry(newJarEntry);
                IOUtils.copy(earInputStream, earOutputStream);
            }
        }
    }

    private static void addMonitorToWar(JarInputStream warInputStream,
            JarOutputStream warOutputStream, File shadedRuntimeJarFile,
            List<File> shadedProbeJarFiles) throws IOException {

        JarEntry warEntry;
        while ((warEntry = warInputStream.getNextJarEntry()) != null) {

            if (warEntry.getName().equals("WEB-INF/web.xml")) {

                LOGGER.info("modifying web.xml ..");

                JarEntry newJarEntry = new JarEntry(warEntry.getName());
                warOutputStream.putNextEntry(newJarEntry);

                // modify web.xml (add servlet and servlet-mapping for admin servlet)
                String originalWebxmlString = IOUtils.toString(warInputStream);
                if (!originalWebxmlString.contains("<filter-name>jmonitor</filter-name>")) {

                    WebXml webXml = new WebXml(originalWebxmlString);

                    webXml.addFilter("jmonitor", "org.jmonitor.ui.server.MonitorUiFilter",
                            "the filter below was added by the jmonitor installer",
                            "the filter above was added by the jmonitor installer");

                    webXml.addFilterMapping("jmonitor", "/jmonitor/*",
                            "the filter-mapping below was added by the jmonitor installer",
                            "the filter-mapping above was added by the jmonitor installer");

                    IOUtils.write(webXml.toString(), warOutputStream);

                } else {

                    // the jmonitor filter mapping is already present in the web.xml, presumably
                    // because the ear/war was built to run jmonitor via load time weaving
                    // and now we are compile time weaving the same ear/war
                    // re-running jmonitor installer (compile time weaving) is not good, but this
                    // condition is caught by inspecting the WEB-INF/lib directory for an existing
                    // jmonitor-runtime.jar (when the ear/war file is built for load time weaving
                    // it will have a file jmonitor-runtime-<version>.jar instead so we detect
                    // that condition previously and replace it with jmonitor-runtime.jar previously
                    // during the installation process)
                    IOUtils.write(originalWebxmlString, warOutputStream);
                }

            } else if (LTW_RUNTIME_JAR_RESOURCE_PATTERN.matcher(warEntry.getName()).matches()) {
                // TODO revisit support for LTW

                // we just skip this entry since we are going to install a new
                // jmonitor-runtime jar later on in the installation process
                continue;

            } else if (isRuntimeJarFile(warEntry)) {

                String message =
                        "found old WEB-INF/lib/" + RUNTIME_JAR_RESOURCE_NAME
                                + ", cannot run installer on already woven ear/war";
                LOGGER.error(message);
                throw new IllegalStateException(message);

            } else {

                JarEntry newJarEntry = new JarEntry(warEntry.getName());
                warOutputStream.putNextEntry(newJarEntry);
                IOUtils.copy(warInputStream, warOutputStream);
            }
        }

        LOGGER.info("adding WEB-INF/lib/" + RUNTIME_JAR_RESOURCE_NAME + " ..");

        JarEntry shadedRuntimeJarEntry =
                new JarEntry("WEB-INF/lib/" + shadedRuntimeJarFile.getName());
        warOutputStream.putNextEntry(shadedRuntimeJarEntry);
        StreamUtils.copy(shadedRuntimeJarFile, warOutputStream);

        for (File shadedProbeJarFile : shadedProbeJarFiles) {
            JarEntry shadedProbeJarEntry =
                    new JarEntry("WEB-INF/lib/" + shadedProbeJarFile.getName());
            warOutputStream.putNextEntry(shadedProbeJarEntry);
            StreamUtils.copy(shadedProbeJarFile, warOutputStream);
        }
    }

    private static boolean isRuntimeJarFile(JarEntry warEntry) {
        String jarName = StringUtils.substringAfter(warEntry.getName(), "WEB-INF/lib/");
        return jarName.equals(RUNTIME_JAR_RESOURCE_NAME);
    }
}
