BrailleZephyr
=============


Licence
=======
BrailleZephyr is licenced under the Apache 2.0 licence.  A copy of the licence
is included with the software.  For details see the file LICENCE.

The BrailleZephyr fonts are licenced under the SIL Open Font License 1.1.  A
copy of the licence is included with the software.  For details see the file
OFL.txt.


Building
========
Gradle is the build system for BrailleZephyr.  Gradle does not need to be
installed as there is a wrapper that is included with the software that will
download Gradle automatically.  You issue build tasks using the wrapper script,
which is gradlew on *nix systems and gradlew.bat on Windows systems.

Use the commands below to build the specific jar file for your platform.  The
jar files are fat jars and should not need any addition dependencies be
self-contained.

To build a jar file for a specific platform:
./gradlew linux64Jar
./gradlew linux32Jar
./gradlew win64Jar
./gradlew win32Jar
./gradlew macxJar

To build all jars for all supported platforms:
./gradlew allJars

Other tasks include:

To build BrailleZephyr:
./gradlew build

To build and run BrailleZephyr:
./gradlew run

To clean the distribution:
./gradlew clean

To obtain a complete list of tasks available:
./gradlew tasks --all


Miscellaneous
=============

Margin bell:
http://www.freesound.org/people/ramsamba/sounds/318687/

Page bell:
http://www.get-sounds.com/
