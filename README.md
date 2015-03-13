# test-dsl-provider
An extension to the Surefire Maven plugin powered by an easy to use DSL filter
## Building the project
Simply run `mvn install`
## See Grammar
See https://github.com/osklyarenko/test-dsl-provider/blob/master/src/test/java/morning/dsl/DslSpec.groovy for details
## Configuring maven project
Add the following entry for for the Surefire plugin under `plugin/dependencies` path
```
<dependency>
    <groupId>morning</groupId>
    <artifactId>test-dsl-provider</artifactId>
    <version>1.0-SNAPSHOT</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.maven.surefire</groupId>
            <artifactId>surefire-junit47</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
## Launching JUnit tests
To launch JUnit tests with filter applied pass the `testFilterDsl` argument to maven. Only full file path is allowed.
```
mvn test -DtestFilterDsl=$PWD/testFilter.groovy
```
## Compatibity
The code should work with
* junit 4.11
* maven-surefire-plugin 2.17

## Example configuration
```
'WIP'.suite {
  -'com.product.model'.pkg
  
  +'demo-api'.module {
    -'ExcludedTestClass'
  }
}
```
This configuration will
* Run all all tests in module `demo-api`
* Not run tests from other modules
  * Not run the `ExcludedTestClass` test
* Filter out all test classes that sit in `com.product.model` package
