set PATH=C:\app\apache\ant\1.8.1\bin;%PATH%
set JAVA_HOME=C:\appw\oracle\jdk\1.5.0_22
set PATH=%JAVA_HOME%\bin;%PATH%

cd build
ant clean
ant
ant src
cd ..

mvn install:install-file -DgroupId=org.aspectj -DartifactId=aspectjtools -Dversion=1.6.11-DEV-20101214 -Dpackaging=jar -Dfile=aj-build/dist/tools/lib/aspectjtools.jar -DgeneratePom=true
mvn install:install-file -DgroupId=org.aspectj -DartifactId=aspectjrt -Dversion=1.6.11-DEV-20101214 -Dpackaging=jar -Dfile=aj-build/dist/tools/lib/aspectjrt.jar -DgeneratePom=true
mvn install:install-file -DgroupId=org.aspectj -DartifactId=aspectjweaver -Dversion=1.6.11-DEV-20101214 -Dpackaging=jar -Dfile=aj-build/dist/tools/lib/aspectjweaver.jar -DgeneratePom=true

mvn install:install-file -DgroupId=org.aspectj -DartifactId=aspectjtools -Dversion=1.6.11-DEV-20101214 -Dpackaging=jar -Dclassifier=sources -Dfile=aj-build/src/aspectjtoolsDEVELOPMENT-src.jar
mvn install:install-file -DgroupId=org.aspectj -DartifactId=aspectjrt -Dversion=1.6.11-DEV-20101214 -Dpackaging=jar -Dclassifier=sources -Dfile=aj-build/src/aspectjrtDEVELOPMENT-src.jar
mvn install:install-file -DgroupId=org.aspectj -DartifactId=aspectjweaver -Dversion=1.6.11-DEV-20101214 -Dpackaging=jar -Dclassifier=sources -Dfile=aj-build/src/aspectjweaverDEVELOPMENT-src.jar
