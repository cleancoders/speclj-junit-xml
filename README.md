# Speclj Junit XML Reporter

*[Speclj](https://github.com/slagyr/speclj) reporter allowing for export of specs to the [JUnit XML Format](https://www.ibm.com/docs/en/developer-for-zos/14.1?topic=formats-junit-xml-format)*

## Installation

### deps.edn
Include in `:deps` section with the `SHA` of the commit you want
```clojure
{
 :deps {
          com.cleancoders.speclj-junit-xml  {:git/url "https://github.com/cleancoders/speclj-junit-xml" :git/sha "[latest-commit-sha]"}
        }
 }
```

## Usage

When the project is loaded as a dependency, `speclj` will dynamically load the reporter when running specs via the `-f` flag:

```shell
clj speclj.main -f junit-xml-reporter
```

This will send the resultant XML to stdout:

```xml
<?xml version="1.0" encoding="UTF-8"?>
  <testsuites id="20230805165531" name="2023-08-05 16:55:31+0000" tests="2" failures="0" time="0.810">
    <testsuite id="1" name="core" tests="2" failures="0" time="0.810">
        <testcase id="2" name="asserts again" time="0.304"></testcase>
        <testcase id="4" name="nested asserts" time="0.506"></testcase>
    </testsuite>
</testsuites>
```