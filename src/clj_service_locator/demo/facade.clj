(ns clj-service-locator.demo.facade
  (:require [clj-service-locator.core :as services]))

(def ^{:private true} service-key :facade)
(def ^{:private true} service-default-ns "clj-service-locator.demo.human")
(def ^{:private true} service-func-names [:greet :scientific-name])

;; initialization of service key, service default namespace, and service allowed function names
(services/register-namespace! service-key service-default-ns service-func-names true)

(defn set-namespace!
  "Sets the namespace via the string :service-ns with the optional :force? argument to force setting even if not all functions are resolvable."
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
  "Function stub for calling :scientific-name in the :dispatcher service  mapping."
  []
  (services/call service-key :scientific-name))
