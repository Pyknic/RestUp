# RestUp
A lightweight Java library to simplify communicating with REST API:s. The library is completely asynchronous with full support for modern constructs such as `CompletableFuture` and `Optional`. 

### Installation
Add the following to your `pom.xml`-file:
```xml
<dependency>
    <groupId>com.github.pyknic</groupId>
    <artifactId>restup</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Connect to an API
```java
Rest rest = Rest.connect("127.0.0.1", 8080);

CompletableFuture<Response> future = rest.post("user", 
    param("firstname", "Adam"), 
    param("lastname", "Adamsson")
);

future.get();
```

### The following Http-commands are supported
```java
rest.get(...);
rest.post(...);
rest.put(...);
rest.delete(...);
rest.options(...);
```

### Upload large amounts of data
Can easily be combined with the `LineIterator` class from the Apache Commons library:
```java
LineIterator it = FileUtils.lineIterator(theFile, "UTF-8");
CompletableFuture<Response> future;
try {
    future = rest.put("article", it);
} finally {
    LineIterator.closeQuietly(it);
}
future.get();
```

### License
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
