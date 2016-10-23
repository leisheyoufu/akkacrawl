# AkkaCrawl

Html crawler is a command line tool based on akka, guice, httpasyncclient etc. It read the json format configuration
file and parse the target site with different template. AkkaCrawl is plugin able design which make it easy to extend the
parser plugin and export plugin to add support for different site.

## Fetch the source code

```
git clone https://github.com/leisheyoufu/akkacrawl.git
```

## Build distribution package

```
cd akkacrawl
gradle release
```
The package is under build/distributions/ directory.


## Run the AkkaCrawl

```
tar xvf akkacrawl-0.1.jar
cd akkacrawl
bin/akkacrawl [-c config/akkacrawl.json]
```
