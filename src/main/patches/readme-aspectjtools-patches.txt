# how to create the patch

cvs diff modules\util\src\org\aspectj\util\FileUtil.java > aspectjtools-1.6.8-FileUtil-closing-zipfile.patch
cvs diff modules\org.aspectj.matcher\src\org\aspectj\weaver\World.java > aspectjtools-1.6.8-World-fixing-typedemotion-config.patch
cvs diff modules\org.aspectj.matcher\src\org\aspectj\weaver\patterns\PointcutEvaluationExpenseComparator.java > aspectjtools-1.6.8-PointcutEvaluationExpenseComparator-modified-cost-priorities.patch
cvs diff modules\org.aspectj.matcher\src\org\aspectj\weaver\ReferenceType.java > aspectjtools-1.6.8-ReferenceType-removed-costly-coercion-check.patch
cvs diff modules\org.aspectj.matcher\src\org\aspectj\weaver\ReferenceType.java > aspectjtools-1.6.8-ReferenceType-disabled-derivative-type-accumulation.patch



# how to apply the patch

mkdir aspectjtools-1.6.9-INTERNAL-1
cd aspectjtools-1.6.9-INTERNAL-1

copy "C:\Documents and Settings\trask\.m2\repository\org\aspectj\aspectjtools\1.6.8\aspectjtools-1.6.8.jar" .
copy "C:\Documents and Settings\trask\.m2\repository\org\aspectj\aspectjtools\1.6.8\aspectjtools-1.6.8-sources.jar" .
copy C:\dev\clearcase\trask_PMON_10_intg\vobs\PerformanceMonitor\jmonitor\src\main\patches\aspectjtools-*.patch .

mkdir aspectjtools-1.6.8-sources
cd aspectjtools-1.6.8-sources
jar xf ..\aspectjtools-1.6.8-sources.jar

set PATH=C:\Program Files (x86)\GnuWin32\bin;%PATH%

patch -p3 < ..\aspectjtools-1.6.8-FileUtil-closing-zipfile.patch
patch -p3 < ..\aspectjtools-1.6.8-World-fixing-typedemotion-config.patch
patch -p3 < ..\aspectjtools-1.6.8-PointcutEvaluationExpenseComparator-modified-cost-priorities.patch
patch -p3 < ..\aspectjtools-1.6.8-ReferenceType-removed-costly-coercion-check.patch
patch -p3 < ..\aspectjtools-1.6.8-ReferenceType-disabled-derivative-type-accumulation.patch

cd ..

mkdir aspectjtools-1.6.8
cd aspectjtools-1.6.8
jar xf ..\aspectjtools-1.6.8.jar

cd ..

set PATH=C:\appw\oracle\jdk\1.5.0_22\bin;%PATH%

javac -classpath aspectjtools-1.6.8.jar -d aspectjtools-1.6.8 aspectjtools-1.6.8-sources\org\aspectj\util\FileUtil.java
javac -classpath aspectjtools-1.6.8.jar -d aspectjtools-1.6.8 aspectjtools-1.6.8-sources\org\aspectj\weaver\World.java
javac -classpath aspectjtools-1.6.8.jar -d aspectjtools-1.6.8 aspectjtools-1.6.8-sources\org\aspectj\weaver\patterns\PointcutEvaluationExpenseComparator.java
javac -classpath aspectjtools-1.6.8.jar -d aspectjtools-1.6.8 aspectjtools-1.6.8-sources\org\aspectj\weaver\ReferenceType.java

jar cf aspectjtools-1.6.9-INTERNAL-1.jar -C aspectjtools-1.6.8 .
jar cf aspectjtools-1.6.9-INTERNAL-1-sources.jar -C aspectjtools-1.6.8-sources .

mvn install:install-file -DgroupId=org.aspectj -DartifactId=aspectjtools -Dversion=1.6.9-INTERNAL-1 -Dpackaging=jar -Dfile=aspectjtools-1.6.9-INTERNAL-1.jar -DgeneratePom=true
mvn install:install-file -DgroupId=org.aspectj -DartifactId=aspectjtools -Dversion=1.6.9-INTERNAL-1 -Dpackaging=jar -Dclassifier=sources -Dfile=aspectjtools-1.6.9-INTERNAL-1-sources.jar
