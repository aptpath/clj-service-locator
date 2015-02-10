# clj-service-locator

A Clojure library designed to provide the Service Locator pattern in an 
idiomatic manner.

## Rationale

The 
[Service Locator](http://en.wikipedia.org/wiki/Service_locator_pattern) 
pattern is a very useful design pattern to provide decoupling services 
and functionality without the need to change calling code.

Although the Service Locator design pattern might be considered only an 
object-oriented (OO) pattern, the pattern is implementated functionally 
in `clj-service-locator` via mapping of a namespace containing service
functions using service keys.

The `clj-service-locator` library provides a light-weight, simple, and 
powerful mechanism for Service Locator (registry).

## Service Locator Uses

The Service Locator pattern is useful in providing flexibility to any 
application, such as allowing for different database solutions for 
storing application data or allowing different authentication options, 
all without changing core application code.

Another Service Locator use is to provide mock services while testing, 
such as eliminating the need for an external test database by mocking up 
an internal test harness database or any other external service that may 
not be available or convenient in the development, testing, and/or 
continuous integration (CI) environments.

## Performance

In `clj-service-locator`, the service function resolutions occur only 
during initial registration and any subsequent service namespace 
changes.  Each service call to the functions is a simple `apply` on the 
resolved functions with the arguments (if any) so no further resolution 
is needed.

Some other approaches require function resolutions dynamically (on every 
call) which is resource-intensive and unnecessary.

## Usage

### Dependencies

```clojure
[clj-service-locator "0.3.0"]
```

The predecessor clojars group `org.clojars.clj-ioc` and `clj-ioc` are 
deprecated and replaced by `clj-service-locator`, please use the 
canonical `clj-service-locator` form as shown above.

### Architectural Suggestions
The most straight-forward implementation of `clj-service-locator` is to 
implement the 
[Facade design pattern](http://en.wikipedia.org/wiki/Facade_pattern) 
via dispatcher namespaces that provide an abstraction layer between 
calling code and the Service Locator and execution of functions.

For example, if you would like to abstract the logging mechanism from the 
domain logic / application code, then you would create a Facade namespace 
`(ns foo.bar.service.logger ...)` as follows:

```clojure
(ns foo.bar.service.logger
  (:require [clj-service-locator.core :as services]))

(def ^{:private true} service-key :logging)
(def ^{:private true} service-default-ns "foo.bar.service.logger.log4j")
(def ^{:private true} service-func-names [:debug :info :warn :error :fatal])

;; initialization of service key, service default namespace, 
;; and service allowed function names
(services/register-namespace! service-key 
                              service-default-ns 
                              service-func-names 
                              true)

(defn set-logging-namespace!
  [service-ns & [force?]]
  (services/set-namespace! service-key service-ns force?)))

(defn debug [m & [x]] (services/call service-key :debug m x)))
(defn info [m & [x]] (services/call service-key :info m x)))
(defn warn [m & [x]] (services/call service-key :warn m x)))
(defn error [m & [x]] (services/call service-key :error m x)))
(defn fatal [m & [x]] (services/call service-key :fatal m x)))
```

And the calls from application namespaces to the logging facade namespace:

```clojure
(ns foo.bar.alpha
  (:require [foo.bar.service.logging :as log]))
  
(defn some-process 
  [x y z] 
  (log/info (str "inside foo.bar.alpha/process with x: " x ";y: " y ";z: " z)))
```

```clojure
(ns foo.bar.beta
  (:require [foo.bar.service.logging :as log]))
  
(defn some-other-process 
  [a b c] 
  (log/warn (str "inside foo.bar.beta/process with a: " a ";b: " b ";c: " c)))
```

The abstraction of services into a facade namespace (foo.bar.service.logger) 
backed by a service locator namespace (clj-service-locator.core) provides
decoupling of the calling functions from the implementation of the
service function(s), allowing the service namespaces to be swapped as
necessary, even at runtime.

Another example might be mocking a storage service for testing:

```clojure
(ns foo.bar.model.store
  (:require [clj-service-locator :as services]))

(services/register-namespace! :store "foo.bar.model.store.db.mongo" [:retrieve :upsert])

(defn set-store-namespace! 
  [service-ns]
  (services/set-namespace! :store service-ns)

(defn upsert
  [type document]
  (services/call :upsert type document))
  
(defn retrieve
  [type key]
  (services/call :retrieve type key))
```

And in the test initialization:

```clojure
(ns foo.bar.model.store.aggregation-test
  (:require [foo.bar.model.store :as store]
            [foo.bar.agent.pa :as pa]))

(store/set-store-namespace! "foo.bar.model.store.db.mem")

(deftest simple-persist-and-aggregate
  ;; test pa persist to mem db and aggregation in mem db without external dep
  )
```  

### Developer Notes
* library renamed to `clj-service-locator` from `clj-ioc` to be more accurate
* some function and demo names were changed between 0.1.6 and 0.2.0 to be more consistent and concise

### Demo Namespaces / Examples

**dispatcher.clj**
```clojure
(ns clj-service-locator.demo.dispatcher
  (:require [clj-service-locator.core :as services]))

(def ^{:private true} service-key :dispatcher)
(def ^{:private true} service-default-ns "clj-service.demo.human")
(def ^{:private true} service-func-names [:greet :scientific-name])

;; initialization of service key, service default namespace, and service allowed function names
(services/register-namespace! service-key service-default-ns service-func-names true)

(defn set-namespace!
  "Sets the namespace via the string :service-ns with the optional :force? 
   argument to force setting even if not all functions are resolvable."
  ([service-ns] (set-namespace! service-ns false))
  ([service-ns force?]
   (services/set-namespace! service-key service-ns force?)))

(defn hello
  "Function to show different function name is able to call an service function."
  ([] (hello nil))
  ([n]
   (services/call service-key :greet n)))

(defn greet
  "Function stub for calling :greet in the :dispatcher service mapping."
  ([] (greet nil))
  ([n]
   (services/call service-key :greet n)))

(defn scientific-name
  "Function stub for calling :scientific-name in the :dispatcher service mapping."
  []
  (services/call service-key :scientific-name))
```

**human.clj**
```clojure
(ns clj-service-locator.demo.human)

(defn greet
  ([] (greet nil))
  ([n] (str "Hi" (if n (str ", " n) "") ".")))

(defn scientific-name
  []
  "Homo sapien")
```

**dog.clj**
```clojure
(ns clj-service-locator.demo.dog)

(defn greet
  ([] (greet nil))
  ([n] (str "Arf" (if n (str ", " n) "") ".")))

(defn scientific-name
  []
  "Canis lupus familiaris")
```

**cat.clj**
```clojure
(ns clj-service-locator.demo.cat)

(defn greet
  ([] (greet nil))
  ([n] (str "Meow" (if n (str ", " n) "") ".")))
```

### REPL Demo

```clojure
user=> (require '[clj-service-locator.core :as services])
nil
user=> (require '[clj-service-locator.demo.dispatcher :as dispatcher])
nil
user=> (services/mapping :dispatcher)
{:ns "clj-service-locator.demo.human", 
 :func-names [:greet :scientific-name], 
 :funcs {:scientific-name #'clj-service-locator.demo.human/scientific-name, 
         :greet #'clj-service-locator.demo.human/greet}}
user=> (dispatcher/greet)
"Hi."
user=> (dispatcher/greet "Abdul")
"Hi, Abdul."
user=> (dispatcher/scientific-name)
"Homo sapien"
user=> (dispatcher/set-namespace! "clj-service-locator.demo.dog")
{:ns "clj-service-locator.demo.dog", 
 :func-names [:greet :scientific-name], 
 :funcs {:scientific-name #'clj-service-locator.demo.dog/scientific-name, 
         :greet #'clj-service-locator.demo.dog/greet}}
user=> (dispatcher/greet)
user=> (dispatcher/greet)
"Arf."
user=> (dispatcher/greet "Abdul")
"Arf, Abdul."
user=> (dispatcher/scientific-name)
"Canis lupus familiaris"
user=> (dispatcher/set-namespace! "clj-service-locator.demo.cat") ;; this will fail
RuntimeException Aborted service namespace assignment to 'clj-service-locator.demo.cat' 
with required functions [greet, scientific-name] due to missing function definitions 
[scientific-name].  clj-service-locator.core/service-ns-map (core.clj:53)
user=> (dispatcher/set-namespace! "clj-service-locator.demo.cat" true) ;; added force? to true to force namespace even if missing functions
{:ns "clj-service-locator.demo.cat", 
 :func-names [:greet :scientific-name], 
 :funcs {:scientific-name #'clj-service-locator.demo.cat/scientific-name, 
         :greet #'clj-service-locator.demo.cat/greet}}
user=> (dispatcher/greet)
"Meow."
user=> (dispatcher/greet "Abdul")
"Meow, Abdul."
user=> (dispatcher/scientific-name)
RuntimeException Exception in :dispatcher service call occurred due to 
no function 'scientific-name' definition in namespace 
'clj-service-locator.demo.cat' (clj-service-locator.demo.cat/scientific-name) 
with no arguments.  clj-service-locator.core/call (core.clj:91)
user=> (dispatcher/set-namespace! "clj-service-locator.demo.human")
{:ns "clj-service-locator.demo.human", 
 :func-names [:greet :scientific-name], 
 :funcs {:scientific-name #'clj-service-locator.demo.human/scientific-name, 
         :greet #'clj-service-locator.demo.human/greet}}
user=> (dispatcher/greet)
"Hi."
```

## Service Function Coverage

There are a variety of functions in the `clj-service-locator` core 
namespace to check for service function coverage.

The best check is to register the namespace to a service key using a 
function name list and not force the registration if any functions are 
not resolvable (found) in the namespace.

```clojure
(services/register-namespace! :foo "clj-service-handler.demo.cat" [:greet :scientific-name])
```
will throw a `RuntimeException` since the namespace 
`clj-service-locator.demo.cat` does NOT define the function 
`scientific-name` and the optional `force?` fourth argument defaults to 
`false`.

```clojure
(services/register-namespace! :foo "clj-service-locator.demo.cat" [:greet :scientific-name] true)
```
will allow the registration even though `clj-service-locator.demo.cat` 
does not define the `scientific-name` function and will not throw an 
exception since the `force?` fourth argument is set to `true`.

However, now the `:foo` service namespace does not have complete 
coverage of the service functions and any call to the `:foo` service 
namespace via the `service/call` to the missing function (in this case 
`scientific-name`) will result in a thrown exception.

It is best practice to *NOT* force registration of partial coverage 
namespaces (those namespaces without complete resolution of *ALL* 
service functions).

## Testing Service Function Coverage

Testing for service functional coverage may be done by:

```clojure
(services/coverages)
```

If the function returns `nil`, then all registered service namespaces 
provide complete coverage for the registered function names.

However, if a map is returned (keyed using the service key), then those 
service keyed namespace in the result map do NOT have complete service 
function coverage.

Individual registered service keys may be checked for missing functions:

```clojure
(services/coverage :service-key)
```

If the result is `nil`, the service namespace has complete coverage (or 
the service key is not registered).  However, if the result is the 
registered map (with the resolved functions redacted), then the 
`:missing-funcs` vector holds the missing function names.

```clojure
user=> (services/register-namespace! :foo "clj-service-locator.demo.cat" [:greet :scientific-name])
RuntimeException Aborted service namespace assignment to 
'clj-service-locator.demo.cat' with required functions 
[greet, scientific-name] due to missing function definitions 
[scientific-name].  clj-service-locator.core/service-ns-map (core.clj:60)
user=> (services/register-namespace! :foo "clj-service-locator.demo.cat" [:greet :scientific-name] true)
{:foo {:ns "clj-service-locator.demo.cat", :func-names [:greet :scientific-name], :funcs {:scientific-name nil, :greet #'clj-service-locator.demo.cat/greet}, :resolved-funcs [:greet], :missing-funcs [:scientific-name]}, :dispatcher {:ns "clj-service-locator.demo.human", :func-names [:greet :scientific-name], :resolved-funcs [:greet :scientific-name], :funcs {:scientific-name #'clj-service-locator.demo.human/scientific-name, :greet #'clj-service-locator.demo.human/greet}}}
user=> (services/coverage :foo)
{:ns "clj-service-locator.demo.cat", :func-names [:greet :scientific-name], :resolved-funcs [:greet], :missing-funcs [:scientific-name]}
user=> (services/coverages)
{:foo {:ns "clj-service-locator.demo.cat", :func-names [:greet :scientific-name], :resolved-funcs [:greet], :missing-funcs [:scientific-name]}}
```

NOTE: `(services/coverages)` is the same call as 
`(services/all-missing-functions)`; and `(services/coverage :service-key)` 
is the same call as `(services/missing-functions :service-key)`.

## Getting service namespace mappings

To get a map of mapped namespaces:

```clojure
(services/get-namespaces)
```

will result in `nil` if no service namespaces are registered.

Or a map with the service key as the key and a string containing the mapped namespace as the value.

For example, if the `:dispatcher` and `:foo` service keys have mapped service namespaces:

```clojure
{:dispatcher "clj-service-locator.demo.human"
 :foo "clj-service-locator.demo.cat"}
```

To get the mapped namespace for a service key:

```clojure
(services/get-namespace :dispatcher)
```

The result is `nil` if there is no mapping, or a namespace as a string.

```clojure
(services/get-namespace :dispatcher)
"clj-service-locator.demo.human"
```

```clojure
(services/get-namespace :no-mapping-to-this-key)
nil
```
## Resources

### Links
* [Service Locator](http://en.wikipedia.org/wiki/Service_locator_pattern) 
* [Facade design pattern](http://en.wikipedia.org/wiki/Facade_pattern) 

## License

Copyright © 2015 [AptPath LLC](http://aptpath.com)

Distributed under the Eclipse Public License, the same as [Clojure](http://clojure.org).
