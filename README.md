# Source code for PraPR

## Build
The source code is written to be compatible with JDK 1.7 and higher. Please use the following command to install the plugin:

```sh
mvn clean install
```

In case you get build error because of lack of some dependency, you are probably using JDK 1.7 and you want to add the command-line option `-Dhttps.protocols=TLSv1.2` to your build command. For example, the following command should work perfectly fine in a system using JDK 1.7:

```sh
mvn clean install -Dhttps.protocols=TLSv1.2
```

In case you get compilation error (e.g., saying signature of a certain method does not match the supplied arguments), probably your local repository contains some of our old JAR files downloaded from Maven Central Repo. Please use the following command before issuing Maven build command (these commands are intended to be executed in an Unix environment; in case Windows Powershell does not recognize these commands, please manually delete folder `pitest` under the folder `.m2\repository\org`).

```sh
rm -rf ~/.m2/repository/org/pitest
```

If you don't want to delete other versions of PITest that you are using, please consider deleting only version 1.3.2 so that the system will get a fresh copy of the JARs from the central repo.

If you are using PraPR for Defects4J, it is preferrable to compile the project using JDK 1.7. To do so, you will need to set JAVA_HOME to home directory of your JDK (e.g. in Unix systems you can use the command template `export JAVA_HOME="/path/to/JDK1.7"`). Please note that in case you build the project using JDK 1.7, you want to use an extra command-line option upon invoking Maven. This is intended to meet the security requirements that are recently in place.

```sh
mvn clean install -Dhttps.protocols=TLSv1.2
```

## Credit

This repository contains source code for PraPR written by Ali Ghanbari over year 2018.

Thanks Lingming Zhang for adding multi-module plugin.

Thanks Samuel Benton for adding Gradle plugin.
