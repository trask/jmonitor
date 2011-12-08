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
package org.jmonitor.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.jmonitor.installer.InstallerMain;

/**
 * @goal weave
 * @phase package
 * @requiresDependencyResolution runtime
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// code heavily borrowed under Apache License 2.0 from Maven Shade Plugin
// (http://maven.apache.org/plugins/maven-shade-plugin)
// specifically version 1.3.1 of org.apache.maven.plugins.shade.mojo.ShadeMojo
// also forking ideas borrowed from maven-surefire-plugin also under Apache License 2.0
public class MonitorMojo extends AbstractMojo
{

    private static final String JMONITOR_INSTALLER_ARTIFACT_NAME =
            "org.jmonitor:jmonitor-installer";

    /**
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * @component
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper;

    /**
     * The destination directory for the woven artifact.
     * 
     * @parameter default-value="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * The name of the woven artifactId.
     * 
     * If you like to change the name of the native artifact, you may use the
     * &lt;build>&lt;finalName> setting. If this is set to something different than
     * &lt;build>&lt;finalName>, no file replacement will be performed, even if
     * wovenArtifactAttached is being used.
     * 
     * @parameter expression="${finalName}"
     */
    private String finalName;

    /**
     * The name of the woven artifactId. So you may want to use a different artifactId and keep the
     * standard version. If the original artifactId was "foo" then the final artifact would be
     * something like foo-1.0.jar. So if you change the artifactId you might have something like
     * foo-special-1.0.jar.
     * 
     * @parameter expression="${wovenArtifactId}" default-value="${project.artifactId}"
     */
    private String wovenArtifactId;

    /**
     * Defines whether the woven artifact should be attached as classifier to the original artifact.
     * If false, the woven jar will be the main artifact of the project
     * 
     * @parameter expression="${wovenArtifactAttached}" default-value="false"
     */
    private boolean wovenArtifactAttached;

    /**
     * The name of the classifier used in case the woven artifact is attached.
     * 
     * @parameter expression="${wovenClassifierName}" default-value="woven"
     */
    private String wovenClassifierName;

    /**
     * The path to the output file for the woven artifact. When this parameter is set, the created
     * archive will neither replace the project's main artifact nor will it be attached. Hence, this
     * parameter causes the parameters {@link #finalName}, {@link #wovenArtifactAttached} and
     * {@link #wovenClassifierName} to be ignored when used.
     * 
     * @parameter
     * @since 1.3
     */
    private File outputFile;

    /**
     * Arbitrary JVM options to set on the command line. This is used for forking and may be removed
     * at a future point when forking is no longer necessary for performance reasons.
     * 
     * @parameter expression="${jvmArgs}"
     * @since 2.1
     */
    @Deprecated
    private String jvmArgs;

    /**
     * Arbitrary AJC options to set on the command line. This is only exposed for debugging.
     * 
     * @parameter expression="${ajcArgs}"
     * @since 2.1
     */
    @Deprecated
    private String ajcArgs;

    /**
     * Map of plugin artifacts.
     * 
     * @parameter expression="${plugin.artifactMap}"
     * @required
     * @readonly
     */
    private Map<String, Artifact> pluginArtifactMap;

    /**
     * Resolves the artifacts needed.
     * 
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * Creates the artifact
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * ArtifactRepository of the localRepository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The plugin remote repositories declared in the POM.
     * 
     * @parameter expression="${project.pluginArtifactRepositories}"
     * @since 2.2
     */
    private List<ArtifactRepository> remoteRepositories;

    /**
     * For retrieval of artifact's metadata.
     * 
     * @component
     */
    private ArtifactMetadataSource metadataSource;

    public void execute() throws MojoExecutionException, MojoFailureException {

        // validation
        if (!"ear".equals(project.getArtifact().getType())
                && !"war".equals(project.getArtifact().getType())) {

            throw new MojoExecutionException("Failed to create woven artifact.",
                    new IllegalStateException(
                            "Project packaging must be ear or war."));
        }

        if (project.getArtifact().getFile() == null) {
            getLog().error("The project main artifact does not exist. This could have the");
            getLog().error("following reasons:");
            getLog().error("- You have invoked the goal directly from the command line. This is");
            getLog().error("  not supported. Please add the goal to the default lifecycle via an");
            getLog().error("  <execution> element in your POM and use \"mvn package\" to have it");
            getLog().error("  run.");
            getLog().error("- You have bound the goal to a lifecycle phase before \"package\".");
            getLog().error("  Please remove this binding from your POM such that the goal will be");
            getLog().error("  run in the proper phase.");
            throw new MojoExecutionException("Failed to create woven artifact.",
                    new IllegalStateException(
                            "Project main artifact does not exist."));
        }

        try {
            executeInternal();
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating woven jar: " + e.getMessage(), e);
        }
    }

    private void executeInternal() throws MojoExecutionException {

        File outputJar = (outputFile == null) ? wovenArtifactFileWithClassifier() : outputFile;

        String jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator
                + "java";

        List<String> classPath = getJmonitorInstallerClassPath();

        Commandline cli = new Commandline();
        cli.setExecutable(jvm);
        cli.createArg().setLine(jvmArgs);
        cli.createArg().setValue("-classpath");
        cli.createArg().setValue(StringUtils.join(classPath.iterator(), File.pathSeparator));
        cli.createArg().setValue(InstallerMain.class.getName());
        cli.createArg().setValue(project.getArtifact().getFile().getAbsolutePath());
        cli.createArg().setValue(outputJar.getAbsolutePath());
        String[] parsedAjcArgs;
        try {
            parsedAjcArgs = CommandLineUtils.translateCommandline(ajcArgs);
        } catch (Exception e) {
            throw new MojoExecutionException("error parsing ajcArgs", e);
        }
        for (String ajcArg : parsedAjcArgs) {
            cli.createArg().setValue("-A" + ajcArg);
        }

        // TODO handle return code
        // int returnCode;
        try {
            int returnCode = CommandLineUtils.executeCommandLine(cli, new DefaultConsumer(),
                    new DefaultConsumer());
            if (returnCode != 0) {
                throw new MojoExecutionException("error while executing jmonitor installer");
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("error while executing jmonitor installer", e);
        }

        // TODO look at how aspectj-maven-plugin handles error reporting and errors

        // InstallerMain.install(project.getArtifact().getFile(), outputJar);

        if (outputFile == null) {

            boolean renamed = false;

            // rename the output file if a specific finalName is set
            // but don't rename if the finalName is the <build><finalName>
            // because this will be handled implicitly later
            if (finalName != null && finalName.length() > 0
                    && !finalName.equals(project.getBuild().getFinalName())) {

                String finalFileName = finalName + "."
                        + project.getArtifact().getArtifactHandler().getExtension();
                File finalFile = new File(outputDirectory, finalFileName);
                replaceFile(finalFile, outputJar);
                outputJar = finalFile;

                renamed = true;
            }

            if (wovenArtifactAttached) {

                getLog().info("Attaching woven artifact.");
                projectHelper.attachArtifact(project, project.getArtifact().getType(),
                        wovenClassifierName, outputJar);

            } else if (!renamed) {

                getLog().info("Replacing original artifact with woven artifact.");
                File originalArtifact = project.getArtifact().getFile();
                replaceFile(originalArtifact, outputJar);
            }
        }
    }

    private List<String> getJmonitorInstallerClassPath() throws MojoExecutionException {

        Artifact jmonitorInstallerArtifact = pluginArtifactMap
                .get(JMONITOR_INSTALLER_ARTIFACT_NAME);

        if (jmonitorInstallerArtifact == null) {
            throw new MojoExecutionException(
                    "Unable to locate jmonitor-installer in the list of plugin artifacts");
        }

        ArtifactResolutionResult result;
        try {
            Artifact originatingArtifact = artifactFactory.createBuildArtifact("dummy", "dummy",
                    "1.0", "jar");
            result = artifactResolver.resolveTransitively(
                    Collections.singleton(jmonitorInstallerArtifact), originatingArtifact,
                    remoteRepositories, localRepository, metadataSource);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("error while resolving "
                    + JMONITOR_INSTALLER_ARTIFACT_NAME + " artifact", e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("error while resolving "
                    + JMONITOR_INSTALLER_ARTIFACT_NAME + " artifact", e);
        }

        List<String> classPath = new ArrayList<String>();
        for (Object artifact : result.getArtifacts()) {
            classPath.add(((Artifact) artifact).getFile().getAbsolutePath());
        }
        return classPath;
    }

    private File wovenArtifactFileWithClassifier() {
        Artifact artifact = project.getArtifact();
        String wovenName = wovenArtifactId + "-" + artifact.getVersion() + "-"
                + wovenClassifierName + "." + artifact.getArtifactHandler().getExtension();
        return new File(outputDirectory, wovenName);
    }

    private void replaceFile(File oldFile, File newFile) throws MojoExecutionException {

        getLog().info("Replacing " + oldFile + " with " + newFile);

        File origFile = new File(outputDirectory, "original-" + oldFile.getName());

        if (oldFile.exists() && !oldFile.renameTo(origFile)) {

            // try a gc to see if an unclosed stream needs garbage collecting
            System.gc();
            System.gc();

            if (!oldFile.renameTo(origFile)) {
                // Still didn't work. We'll do a copy
                try {
                    FileOutputStream fout = null;
                    FileInputStream fin = null;
                    try {
                        fout = new FileOutputStream(origFile);
                        fin = new FileInputStream(oldFile);
                        IOUtil.copy(fin, fout);
                    } finally {
                        IOUtil.close(fin);
                        IOUtil.close(fout);
                    }
                } catch (IOException ex) {
                    // kind of ignorable here. We're just trying to save the original
                    getLog().warn(ex);
                }
            }
        }
        if (!newFile.renameTo(oldFile)) {

            // try a gc to see if an unclosed stream needs garbage collecting
            System.gc();
            System.gc();

            if (!newFile.renameTo(oldFile)) {
                // Still didn't work. We'll do a copy
                try {
                    FileOutputStream fout = null;
                    FileInputStream fin = null;
                    try {
                        fout = new FileOutputStream(oldFile);
                        fin = new FileInputStream(newFile);
                        IOUtil.copy(fin, fout);
                    } finally {
                        IOUtil.close(fin);
                        IOUtil.close(fout);
                    }
                } catch (IOException ex) {
                    throw new MojoExecutionException(
                            "Could not replace original artifact with woven artifact!", ex);
                }
            }
        }
    }
}
