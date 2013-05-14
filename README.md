# ozb-scala-utils

Collection of various scala utility classes.

## Build

Use sbt to build.

### Dependencies

	"com.github.scopt" % "scopt_2.9.2" % "2.1.0" withSources()

required by org.ozb.utils.ivy.*

## Publishing

You can use sbt publish mechanism, ex: publish-local

`> publish-local`

this will publish the library into the ivy local repository.
To use it, add a dependency:

`libraryDependencies += "org.ozb" %% "ozb-scala-utils" % "0.2"`
