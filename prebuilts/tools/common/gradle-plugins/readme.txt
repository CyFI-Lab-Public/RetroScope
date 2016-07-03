repository/ is a Maven repository that contains internal Gradle plugins
necessary to build Gradle projects such as tools/base.

It contains the plugins and their dependencies in order to not require
downloading anything from an external repository.

It is updated by running the following commands:
    $ cd tools/base/misc/distrib_plugins/buildSrc
    $ gradle updatePrebuilts
    $ cd ..
    $ gradle cloneArtifacts