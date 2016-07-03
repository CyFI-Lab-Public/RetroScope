This folder contains 2 repositories which are both needed
to compile tools/base, tools/swt and tools/build with Gradle,
without accessing an external repository.

repository/ is the main repository containing dependencies that
are needed at runtime. A NOTICE file should be put next to each
artifact for the build to succeed.

internal/ is a secondary repository for dependencies necessary
for building and testing but not for running. The content of this
repository is not published.