curl -O http://maven.pietschy.com/repository/com/pietschy/gwt/gwt-pectin/0.8/gwt-pectin-0.8.jar
curl -O http://maven.pietschy.com/repository/com/pietschy/gwt/gwt-pectin/0.8/gwt-pectin-0.8.pom

# install to local repository for testing
mvn install:install-file -DgroupId=org.jmonitor.thirdparty.pietschy -DartifactId=gwt-pectin -Dversion=0.8-SNAPSHOT -Dpackaging=jar -Dfile=gwt-pectin-0.8.jar -DpomFile=gwt-pectin-0.8.pom

# clean local repository before deploying
rm -rf ~/.m2/repository/org/jmonitor/thirdparty/pietschy

mvn deploy:deploy-file -DgroupId=org.jmonitor.thirdparty.pietschy -DartifactId=gwt-pectin -Dversion=0.8-SNAPSHOT -Dpackaging=jar -Dfile=gwt-pectin-0.8.jar -DpomFile=gwt-pectin-0.8.pom -Durl=http://repository.jmonitor.org/content/repositories/snapshots -DrepositoryId=jmonitor-snapshots

# clean up
rm gwt-pectin-0.8.jar
rm gwt-pectin-0.8.pom
