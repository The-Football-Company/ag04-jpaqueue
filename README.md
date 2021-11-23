# ag04-jpaqueue

AG04 database as queue support library.

## Usage
For detailed instruction on how to use this libarary please refer to the following articles:
* [Part 1: Practical queueing using SQL - Rationale and general design](https://medium.com/agency04/practical-queueing-using-sql-part-1-rationale-and-general-design-d180d6848030)
* [Part 2: Practical queueing using SQL - Do it simply using Spring Boot and JPA](https://medium.com/agency04/practical-queueing-using-sql-part-2-do-it-simply-using-spring-boot-and-jpa-e9cb53f91f36)

To use this library in you gradle project simply add the following repository definition to the project build.gradle:

```groovy
    maven {
        url 'https://maven.pkg.github.com/ag04/ag04-jpaqueue'
        credentials {
            username = "dmadunic"
            password = "cd1682474e79efb726afe2dd08f902b3da7aa993"
        }
    }
```
This package is published in github packages gradle repository, yet unfortunatelly due to the lack of public access to the published libraries this hack is needed.

Password is **readonly** personal access token (PAT) to github packages.


### Requirements
* [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

### Setup (First time)
1. Clone the repository: `git clone git@github.com:ag04/ag04-jpaqueue.git`
4. Build project with: ` ./gradlew clean build `

### Release
Make sure that file gradle.properties in the folder ${USER_HOME}/.gradle/ contains the following two variables defined:

* github_username
* github_password : personal github token to be used to install/update packages

1) Commit and push everything
2) `./gradlew release`

And simply follow the instructions on the console

## Changelog

### v. 1.2.1. Configured Google Artifact Registry as maven repository for publishing
- Upgraded gradle wrapper to 7.3.

### v. 1.2.0. Changed QueueConsumerModul method processItem() signature to include count and size params
 - Optional<QueueingState> processItem(ID itemId, int count, int size);
### v. 1.1.0 Java 11, ZonedDateTime
    - Changed LocalDateTime to ZonedDateTime in QueueingState.java and the rest of the library
    - Increased Java build version to 11
### v. 1.0.1 - Configured build to publish to GitHub packages
### v. 1.0.0 - Base implementation

## Credits
* Vjeran Marčinko (Author)
* Domagoj Madunić (Maintener)
