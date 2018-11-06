This project uses the Gradle build system and the Gradle wrapper.

To run Gradle, cd into the root project directory and execute ./gradlew (or .\gradlew.bat on Windows).
The first run of the wrapper will download a local copy of Gradle to your system.

You can then:
- Build and start the application with the 'run' build task (e.g. ./gradlew run).
- Generate Javadoc with the 'javadoc' task (stored in build/docs/javadoc/)
- Create a distributable zip or tar archive with the 'distZip' and 'distTar' tasks respectively (stored in build/distributions/)
- Generate Eclipse project files with the 'eclipse' task
- Open the project directly in IntelliJ IDEA

Alternatively, extract the pre-built distribution at extra/cs1013-1.0-SNAPSHOT.zip to a suitable location and run bin/cs1013 
(or bin/cs1013.bat on Windows). Pre-generated Javadoc is also provided in the 'extra' directory.

Note:
- An internet connection is required to run the application as it connects to a remote MySQL database
- This project makes use of Java 8 features (such as lambdas)
- 
