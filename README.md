# Jenkins Kafka Logs Plugin

This plugin adds support for wrapping pipeline project logs in a build wrapper that ships them to kafka.

This plugin is available [here](http://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/kafkalogs/)
and has [a page](https://wiki.jenkins-ci.org/display/JENKINS/Kafka+Logs+Plugin) on the Jenkins Wiki.

## Using in pipeline workflows

The build wrapper can be used to ship logs to kafka server and topic.

E.g., pipeline job "test" with build 40 ran with the following pipeline script:

```groovy
node {
    wrap([$class: 'KafkaBuildWrapper', kafkaServers: 'host1.example.com:9092,host2.example.com:9092', kafkaTopic: 'buildlogs']) {
        echo 'Hello World'
        echo 'Oh Hello'
        echo 'Finally'
    }
}
```

Would produce messages on kafka topic "buildlogs":

```
{"build":40,"job":"test","message":"Hello World"}
{"build":40,"job":"test","message":"Oh Hello"}
{"build":40,"job":"test","message":"Finally"}
```


Copyright O.C. Tanner 2018, https://www.octanner.com. See LICENSE for more information.  Work was derived from the sources of https://github.com/jenkinsci/ansicolor-plugin Thanks to Daniel Dobrovkine for providing a great color ansi plugin.