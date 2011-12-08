/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmonitor.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmonitor.installer.util.FormatUtils;
import org.jmonitor.installer.util.StreamUtils;
import org.jmonitor.installer.util.StreamUtils.JarStreamCallback;
import org.jmonitor.installer.util.TempDirectoryUtils;
import org.jmonitor.installer.util.TempDirectoryUtils.TempDirectoryCallback;
import org.jmonitor.installer.util.WeaverUtils;

/**
 * Main installer entry point.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class InstallerMain {

    // this is used so that war/ear files can be built for load time weaving, but can still later be
    // compile time woven
    // TODO generalize pattern to match deployed snapshot versions, e.g. "1.0-20100502.191338-3"
    private static final Pattern JMONITOR_RUNTIME_LTW_JAR_RESOURCE_PATTERN =
            Pattern.compile("jmonitor-runtime-\\d+\\.\\d+(?:\\.\\d+)?(?:-SNAPSHOT)?.jar");
    private static final String JMONITOR_RUNTIME_JAR_RESOURCE_NAME = "jmonitor-runtime.jar";
    private static final String JMONITOR_ASPECTS_JAR_RESOURCE_NAME = "jmonitor-aspects.jar";
    private static final String JAVAEE_JAR_RESOURCE_NAME = "javaee-api.jar";

    private static final Log LOG = LogFactory.getLog(InstallerMain.class);

    // utility class
    private InstallerMain() {}

    public static void main(String[] args) throws IOException {
        runMain(args, true);
    }

    public static void runMain(String[] args, boolean useSystemExit) throws IOException {
        String inputFilename = null;
        String outputFilename = null;
        List<String> ajcArgs = new ArrayList<String>();
        for (String arg : args) {
            if (arg.startsWith("-A")) {
                // ajc arg
                ajcArgs.add(arg.substring(2));
            } else if (inputFilename == null) {
                // first non-ajc arg is input file
                inputFilename = arg;
            } else if (outputFilename == null) {
                // second non-ajc arg is output file
                outputFilename = arg;
            } else {
                // too many non-ajc args
                printUsageAndExit(useSystemExit);
                return;
            }
        }

        if (inputFilename == null) {
            printUsageAndExit(useSystemExit);
            return;
        }

        if (!inputFilename.endsWith(".ear") && !inputFilename.endsWith(".war")) {
            // TODO generalize this to all errors and useSystemExit appropriately
            LOG.error("ear/war filename must end with .ear or .war");
            return;
        }
        if (args.length == 2) {
            outputFilename = args[1];
        } else {
            String baseName = FilenameUtils.getBaseName(inputFilename);
            String extension = FilenameUtils.getExtension(inputFilename);
            outputFilename = baseName + "-woven." + extension;
        }

        install(new File(inputFilename), new File(outputFilename), ajcArgs);
    }

    private static void printUsageAndExit(boolean useSystemExit) {
        String message = "Usage: java " + InstallerMain.class.getName()
                + " <input ear/war file> [output ear/war file]";
        LOG.fatal(message);
        if (useSystemExit) {
            System.exit(1);
        } else {
            throw new IllegalArgumentException(message);
        }
    }

    private static void install(final File inputFile, final File outputFile,
            final List<String> ajcArgs) throws IOException {

        long startMillis = System.currentTimeMillis();

        TempDirectoryUtils.execute("jmonitor-installer-main-", new TempDirectoryCallback() {
            public void doWithTempDirectory(File tempDirectory) throws IOException {
                installInternal(inputFile, outputFile, ajcArgs, tempDirectory);
            }
        });

        long endMillis = System.currentTimeMillis();
        Date endDate = new Date(endMillis);
        LOG.info("completed in " + FormatUtils.formatAsMinutesAndSeconds(endMillis - startMillis));
        LOG.info("completed at " + DateFormat.getDateTimeInstance().format(endDate));
    }

    private static void installInternal(File inputFile, File outputFile, List<String> ajcArgs,
            File tempDirectory) throws IOException, FileNotFoundException {

        File tempWovenFile = new File(tempDirectory, outputFile.getName());

        // extract aspects
        extractResource(JMONITOR_RUNTIME_JAR_RESOURCE_NAME, tempDirectory);
        final File jmonitorJarFile = new File(tempDirectory, JMONITOR_RUNTIME_JAR_RESOURCE_NAME);
        File aspectJarFile = new File(tempDirectory, JMONITOR_ASPECTS_JAR_RESOURCE_NAME);

        // create small jar with only aspect, this helps speed up aspectj(?)
        StreamUtils.execute(jmonitorJarFile, aspectJarFile, new JarStreamCallback() {
            public void doWithJarStreams(JarInputStream jarInputStream,
                    JarOutputStream jarOutputStream) throws IOException {

                JarEntry jarEntry;
                while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                    if (jarEntry.getName().matches("org/jmonitor/extension/probe/.*\\.class")) {
                        // found aspect
                        // TODO implement better matching strategy that actually looks at class file
                        // to see if it is an aspect
                        JarEntry newJarEntry = new JarEntry(jarEntry.getName());
                        jarOutputStream.putNextEntry(newJarEntry);
                        IOUtils.copy(jarInputStream, jarOutputStream);
                    }
                }
            }
        });

        final String[] aspectPath = new String[] { aspectJarFile.getPath() };

        // extract Xlint.properties which is used during aspectj compilation
        extractResource("Xlint.properties", tempDirectory);
        final File xlintPropertiesFile = new File(tempDirectory, "Xlint.properties");

        final List<String> mergedAjcArgs = new ArrayList<String>();
        mergedAjcArgs.add("-Xlintfile");
        mergedAjcArgs.add(xlintPropertiesFile.getAbsolutePath());
        // due to the (incomplete?) shading, we get bad version number found warning messages 
        mergedAjcArgs.add("-checkRuntimeVersion:false");
        mergedAjcArgs.addAll(ajcArgs);

        // extract javaee-api.jar which needs to be added to weaving classpath
        extractResource(JAVAEE_JAR_RESOURCE_NAME, tempDirectory);
        File javaeeJarFile = new File(tempDirectory, JAVAEE_JAR_RESOURCE_NAME);
        final String[] aspectClasspath =
                new String[] { jmonitorJarFile.getPath(), javaeeJarFile.getPath() };

        if (inputFile.getName().endsWith(".ear")) {

            LOG.info("weaving ear file " + inputFile.getName() + " ..");

            // weave the ear file
            StreamUtils.execute(inputFile, tempWovenFile, new JarStreamCallback() {
                public void doWithJarStreams(JarInputStream earInputStream,
                        JarOutputStream earOutputStream) throws IOException {
                    WeaverUtils.weaveEar(aspectPath, aspectClasspath, mergedAjcArgs,
                            earInputStream, earOutputStream);
                }
            });

            LOG.info("post-processing ear file " + inputFile.getName() + " ..");

            // post-process the ear file (add jmonitor.jar and modify web.xml)
            StreamUtils.execute(tempWovenFile, outputFile, new JarStreamCallback() {
                public void doWithJarStreams(JarInputStream earInputStream,
                        JarOutputStream earOutputStream) throws IOException {
                    addMonitorToEar(earInputStream, earOutputStream, jmonitorJarFile);
                }
            });

        } else if (inputFile.getName().endsWith(".war")) {

            LOG.info("weaving war file " + inputFile.getName() + " ..");

            // weave the war file
            StreamUtils.execute(inputFile, tempWovenFile, new JarStreamCallback() {
                public void doWithJarStreams(JarInputStream warInputStream,
                        JarOutputStream warOutputStream) throws IOException {
                    WeaverUtils.weaveWar(aspectPath, aspectClasspath, mergedAjcArgs,
                            warInputStream, warOutputStream);
                }
            });

            LOG.info("post-processing war file " + inputFile.getName() + " ..");

            // post-process the war file (add jmonitor.jar and modify web.xml)
            StreamUtils.execute(tempWovenFile, outputFile, new JarStreamCallback() {
                public void doWithJarStreams(JarInputStream warInputStream,
                        JarOutputStream warOutputStream) throws IOException {
                    addMonitorToWar(warInputStream, warOutputStream, jmonitorJarFile);
                }
            });

        } else {

            throw new IllegalStateException("ear/war filename must end with .ear or .war");
        }
    }

    private static void addMonitorToEar(JarInputStream earInputStream,
            JarOutputStream earOutputStream, File jmonitorJarFile) throws IOException {

        JarEntry earEntry;
        while ((earEntry = earInputStream.getNextJarEntry()) != null) {
            if (earEntry.getName().matches("[^/]*\\.war")) {

                LOG.info("post-processing war file " + earEntry.getName() + " ..");

                // found a war file
                JarEntry newJarEntry = new JarEntry(earEntry.getName());
                earOutputStream.putNextEntry(newJarEntry);
                // add monitor to the war file
                JarInputStream warInputStream = new JarInputStream(earInputStream);
                JarOutputStream warOutputStream = new JarOutputStream(earOutputStream);
                addMonitorToWar(warInputStream, warOutputStream, jmonitorJarFile);
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
            JarOutputStream warOutputStream, File jmonitorJarFile) throws IOException {

        JarEntry warEntry;
        while ((warEntry = warInputStream.getNextJarEntry()) != null) {

            if (warEntry.getName().equals("WEB-INF/web.xml")) {

                LOG.info("modifying web.xml ..");

                JarEntry newJarEntry = new JarEntry(warEntry.getName());
                warOutputStream.putNextEntry(newJarEntry);

                // modify web.xml (add servlet and servlet-mapping for admin servlet)
                String originalWebxmlString = IOUtils.toString(warInputStream);
                if (!originalWebxmlString.contains("<servlet-name>jmonitor</servlet-name>")) {
                    String newWebxmlString = addServletMappingToWebxmlString(originalWebxmlString);
                    IOUtils.write(newWebxmlString, warOutputStream);
                } else {
                    // the jmonitor servlet mapping is already present in the web.xml, presumably
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

            } else if (isJmonitorRuntimeLtwJarFile(warEntry)) {

                // this is ok, we just skip this entry since we are going to install a new
                // jmonitor-runtime jar later on in the installation process
                continue;

            } else if (isOldJmonitorRuntimeJarFile(warEntry)) {

                String message =
                        "found old WEB-INF/lib/" + JMONITOR_RUNTIME_JAR_RESOURCE_NAME
                                + ", cannot run installer on already woven ear/war";
                LOG.error(message);
                throw new IllegalStateException(message);

            } else {

                JarEntry newJarEntry = new JarEntry(warEntry.getName());
                warOutputStream.putNextEntry(newJarEntry);
                IOUtils.copy(warInputStream, warOutputStream);
            }
        }

        LOG.info("adding WEB-INF/lib/" + JMONITOR_RUNTIME_JAR_RESOURCE_NAME + " ..");

        JarEntry newJarEntry = new JarEntry("WEB-INF/lib/" + JMONITOR_RUNTIME_JAR_RESOURCE_NAME);
        warOutputStream.putNextEntry(newJarEntry);

        StreamUtils.copy(jmonitorJarFile, warOutputStream);
    }

    private static boolean isOldJmonitorRuntimeJarFile(JarEntry warEntry) {
        String jarName = StringUtils.substringAfter(warEntry.getName(), "WEB-INF/lib/");
        return jarName.equals(JMONITOR_RUNTIME_JAR_RESOURCE_NAME);
    }

    // TODO remove dependency from WeaverUtils and reduce visibility to private
    public static boolean isJmonitorRuntimeLtwJarFile(JarEntry warEntry) {
        String jarName = StringUtils.substringAfter(warEntry.getName(), "WEB-INF/lib/");
        return JMONITOR_RUNTIME_LTW_JAR_RESOURCE_PATTERN.matcher(jarName).matches();
    }

    private static String addServletMappingToWebxmlString(String webxmlString) {

        // try to keep consistent newline in additions to web.xml
        String newline;
        if (webxmlString.contains("\r\n")) {
            newline = "\r\n";
        } else {
            newline = "\n";
        }

        // TODO insert this into the correct location according to DTD
        int index = webxmlString.lastIndexOf("</web-app>");
        String before = webxmlString.substring(0, index);
        String after = webxmlString.substring(index);

        final int stringBufferConservativeSize = 500;
        StringBuffer buffer = new StringBuffer(stringBufferConservativeSize);
        buffer.append(newline);
        buffer.append("<!-- the servlet and servlet-mapping below"
                + " were added by the jmonitor installer -->");
        buffer.append(newline);
        buffer.append("<servlet>");
        buffer.append(newline);
        buffer.append("<servlet-name>jmonitor</servlet-name>");
        buffer.append(newline);
        buffer.append("<servlet-class>org.jmonitor.ui.server.MonitorServlet</servlet-class>");
        buffer.append(newline);
        buffer.append("</servlet>");
        buffer.append(newline);
        buffer.append("<servlet-mapping>");
        buffer.append(newline);
        buffer.append("<servlet-name>jmonitor</servlet-name>");
        buffer.append(newline);
        buffer.append("<url-pattern>/jmonitor/*</url-pattern>");
        buffer.append(newline);
        buffer.append("</servlet-mapping>");
        buffer.append(newline);
        buffer.append(newline);

        return before + buffer + after;
    }

    private static void extractResource(String path, File aspectDir) throws IOException {

        InputStream classInputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        try {
            StreamUtils.copy(classInputStream, new File(aspectDir, path));
        } finally {
            classInputStream.close();
        }
    }
}
