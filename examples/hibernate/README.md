Seam Hibernate Example
=================

This is the Hotel Booking example implemented in Seam and Hibernate POJOs.

Running the example
-------------------

To deploy the example to a running JBoss AS instance, follow these steps:

1. In the example root directory run:

        mvn clean install

2. Set JBOSS_HOME environment property.

3. In the hibernate-web directory run:

        mvn jboss-as:deploy

4. Open this URL in a web browser: http://localhost:8080/hibernate-web


Testing the example
-------------------

This example is covered by integration and functional tests. All tests use the following technologies:

* __Arquillian__ -  as the framework for EE testing, for managing of container lifecycle and deployment of test archive,
* __ShrinkWrap__ - to create the test archive (WAR).


### Integration tests

Integration tests cover core application logic and reside in the EJB module. In addition to Arquillian and ShrinkWrap, the integration tests also use:

* __JUnitSeamTest__ - to hook into the JSF lifecycle and assert server-side state,
* __ShrinkWrap Resolver__ - to resolve dependencies of the project for packaging in the test archive.

_Note: For this test to function properly, it is necessary to run the JBoss AS instance with the **-ea** JVM argument (Enable Assertions)._

The tests are executed in Maven's test phase. By default they are skipped and can be executed on JBoss AS with:

    mvn clean test -Darquillian=jbossas-managed-7

The `JBOSS_HOME` environment variable must be set and point to a JBoss AS instance directory.

To test on a running server, use

    mvn clean test -Darquillian=jbossas-remote-7

### Functional tests

Functional tests are located in a separate project and are not executed during the build of the example. They test the built archive in an application server through browser-testing. They use:

* __Arquillian Graphene Extension__ - an advanced Ajax-capable type-safe Selenium-based browser testing tool,
* __Arquillian Drone Extension__ - to automatically run and stop browser instances.

_Note: It is necessary to first build and install the example, because the functional test resolves the test artifact from the local Maven repository._

Run the functional test on JBoss AS instance with
    
    mvn -f hibernate-ftest/pom.xml clean test

The `JBOSS_HOME` environment variable must be set and point to a JBoss AS instance directory.

To test on a running server, use

    mvn -f hibernate-ftest/pom.xml clean test -Dremote

Testing in JBDS
---------------
### Integration tests

1. Open JBDS and start a configured instance of JBoss AS
2. Import the example project and its submodules
3. In the _Project Explorer_, select the Web module project, then
    1. Type `Ctrl+Alt+P` (_Select Maven Profiles_) and check `integration-tests` and `arq-jbossas-7-remote`
    2. Right-click the module and select _Run As_ - _JUnit Test_

### Functional tests

It is not possible to run the functional tests of this example in JBDS, because they use the maven-dependency-plugin to copy test classes from a different maven artifact, which is not a configuration supported by JBDS.