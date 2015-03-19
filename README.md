![Build status](https://circleci.com/gh/loganbraga/hobbes.svg?style=shield&circle-token=082da7ff9bb0b030b334fd5f456f495058f5bd45)

/!\ WORK IN PROGRESS /!\

# Hobbes

Hobbes is an applicative suite made to greatly improve taking lectures notes. Just take your courses in our resilient, easy and flexible .hob language and compile them all at once to
beautiful, static, local websites allowing for painless review sessions.

## Installation

Download on [Hobbes website](http://hobbes-lang.org). You can download standalone jar, or binary executable.

## Documentation

Coming soon. Stay tuned on [Hobbes website](http://hobbes-lang.org).

## Running

+ With a standalone jar `java -jar path/to/hobbes.jar action [argument] [options]`
+ With binary executable `hobbes action [argument] [options]`, assuming `hobbes` was added to your `$PATH`.
+ Directly from leiningen `lein run -- action [argument] [options]` assuming leiningen and Clojure were installed and available on your `$PATH`.

### Example commands

+ To get help `hobbes help`, `hobbes -h`, `hobbes --help`
+ To compile a folder containing courses in hob format `hobbes compile path/to/the/folder -o destination/path`
+ To compile a folder with specific options `hobbes compile -i path/to/the/folder -output=destination/path -w 420 -u "Your Full Name" --force` (count 420 words per minute for estimated reading time, use "Your Full Name" as author's name, and force compilation of already up-to-date files)
+ To compile a single file and spit its result to STDOUT `hobbes compile path/to/the/file.hob`
+ To dump default config in a directory `hobbes dump destination/path`

## Building from source

If you want to build hobbes, from source, clone this repo, download and install Clojure on your platform, download and install leiningen, and build.
+ `git clone https://github.com/loganbraga/hobbes`
+ `lein deps`
+ `lein with-profile +gui:+cli uberjar` => Compile project to standalone jar, with cli mode and gui mode
+ `lein bin` => Transform jar to binary executable

## Running tests

Run test with leiningen: `lein test`.

## Generating code documentation

Generate documentation with Codox: `lein doc`.

## License

Copyright © 2015 Logan Braga

Distributed under the GNU/GPL Public License version 3. See LICENSE for more details.
