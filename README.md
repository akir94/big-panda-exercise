# big-panda-exercise

Implementation of an exercise.

### Structure

This project is a single java process, that does the following:
* Launches the gnerator process
* Captures its output and pipes it into an Akka stream
* Launches an http server

### Running and Configuration
This project is launched as an ordinary java process.
It can be configured through environment variables:

* GENERATOR_FILE: the path to the generator process file, defaults to "generator-windows-amd64.exe".
* HTTP_SERVER_PORT: the port to vind the http server to, defaults to 8500.

### Further Improvement
* Add unit tests.
* Separate the Akka stream and http server into different processes and set up a databse between them.
* Switch to a more convenient framework for the http server (and remove the need for manually parsing the request).
