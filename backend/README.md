Empire Backend for App Engine Standard (Java 8)
============================

See the [Google App Engine standard environment documentation][ae-docs] for more
detailed instructions.

[ae-docs]: https://cloud.google.com/appengine/docs/java/


* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Gradle](https://gradle.org/gradle-download/)
* [Google Cloud SDK](https://cloud.google.com/sdk/) (aka gcloud)

## Setup

• Download and initialize the [Cloud SDK](https://cloud.google.com/sdk/)

    gcloud init

* Create an App Engine app within the current Google Cloud Project

    gcloud app create

## Gradle
### Running locally

    gradle appengineRun

To use vist: http://localhost:8080/

### Deploying

    gradle appengineDeploy

To use vist:  https://YOUR-PROJECT-ID.appspot.com


# Developers
## Testing
    `gradle test`

## Todos
### Suggested enums
 - Army tags
 - Nation tags
 - Noble tags

### Armies
 - Might be beneficial for class to have a constructor or static creation method

### Regions
 - Separate methods for fortification percentage display vs army strength modification
