# ozb-scala-utils

Collection of various scala utility classes.

## Build

Use sbt to build.
Some of the dependencies are not available in public repositories so you
have to install them in your local repository.

To compile the project, run sbt than issue a compile command

`> compile`

### Dependencies by package

- org.ozb.utils.google.gdata : google=api-client, gdata-core, gdata-docs, gdata-spreadsheet
- org.ozb.utils.io  : none
- org.ozb.utils.ivy : none
- org.ozb.utils.jarfinder : scopt
- org.ozb.utils.maven : none
- org.ozb.utils.osx : none

## Publishing

You can use sbt publish mechanism, ex: publish-local

`> publish-local`

this will publish the library into the ivy local repository.
To use it, add a dependency:

`libraryDependencies += "org.ozb" %% "ozb-scala-utils" % "0.1"`
