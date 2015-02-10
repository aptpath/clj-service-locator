(ns clj-service-locator.core
  (:require [clojure.set :as set]))

(def ^{:private true} service-namespaces (atom {}))

(defn- keyword-or-string
  [s]
  (cond (keyword? s) (name s)
        :else (str s)))

(def ^{:private true} kws (memoize keyword-or-string))

(defn- kw
  [s]
  (cond (keyword? s) s
        (string? s) (keyword s)
        :else (keyword (str s))))

(defn- vec-
  "Returns the vector resulting in v1 minus v2."
  [v1 v2]
  (into [] (set/difference (into #{} v1) (into #{} v2))))

(defn- nil-map-values
  "Returns the list of keys in map :m with nil values. If map has no nil values, returns nil."
  [m]
  (reduce #(if ((first %2) m) % (conj % (first %2))) nil m))

(defn- nice-list
  "Returns a string of a given list of keywords/strings as strings separated by commas."
  [l]
  (clojure.string/join ", " (reduce #(conj % (kws %2)) [] l)))

(defn resolve-func
  "Returns a resolved function given a namespace and function name, if present.  If function doesn't exist, returns nil."
  [fns fname]
  (require (symbol (kws fns)))
  (resolve (symbol (str (kws fns) "/" (kws fname)))))

(defn service-ns-map
  "Creates a service namespace map with resolved functions given 
  function names and service namespace.  Throws an exception if missing 
  a function AND the force? parameter is missing, nil, or set to false."
  [service-ns service-func-names & [force?]]
  (let [res (reduce #(assoc % %2 (resolve-func service-ns %2))
                    {}
                    service-func-names)
        missing-funcs (nil-map-values res)
        mfs (into [] missing-funcs)]
    (if-not (empty? missing-funcs)
      (if force?
        {:ns (kws service-ns)
         :func-names service-func-names
         :funcs res
         :resolved-funcs (vec- service-func-names mfs)
         :missing-funcs mfs}
        (throw (RuntimeException. (str "Aborted service namespace assignment to '" (kws service-ns)
                                       "' with required function(s) [" (nice-list service-func-names) "] due to missing function definition(s) ["
                                       (nice-list mfs) "]."))))
      {:ns (kws service-ns)
       :func-names service-func-names
       :resolved-funcs service-func-names
       :funcs res})))

(defn mappings
  "Returns the current service-namespaces map."
  []
  @service-namespaces)

(defn mapping
  "Returns the service namespace map for entry with service-key."
  [service-key]
  (@service-namespaces service-key))

(defn- get-missing-funcs
  "Arguments:
    service-key (optional) - the service key for the service mapping missing function retrieval
                         if not nil, return only the missing functions info map for the service key mapping.
                         if nil, return all service mappings with missing functions as service mapping.

  Returns:
    nil - if there are no service mapping(s) with missing (unresolvable) functions
    single service info mapping - if there is an service-key AND the service-key mapping is missing functions
    service key map with service info mapping - if there is no service-key AND at least one (1) service mapping is missing functions

  Service missing info mapping keys:
     :ns - namespace
     :func-names - vector of function names
     :resolved-funcs - vector of resolved (successful) function names
     :missing-funcs - vector of missing function names."
  [& [service-key]]
  (if service-key
    (if (:missing-funcs (@service-namespaces service-key))
      (dissoc (@service-namespaces service-key) :funcs))
    (reduce #(if (:missing-funcs (@service-namespaces %2))
              (assoc % %2 (dissoc (@service-namespaces %2) :funcs))
              %)
            nil
            (keys @service-namespaces))))

(defn all-missing-functions
  "Returns all service namespaces as maps with servy keys as keys and list of missing function names.
   If a service key has no missing functions (meaning the namespace has complete coverage of
   the service functions), then the service key is not part of the result.
   If the result is nil, then no registered service keys are missing any functions and there is
   complete coverage of all registered service keys."
  []
  (get-missing-funcs))

(defn missing-functions
  "Returns a list of missing functions (not defined in the registered namespace) of the
  registered service-key parameters.  Returns nil is the service-key doesn't exist in the
  registry and/or if the service-key namespace has complete coverage of the service functions."
  [service-key]
  (get-missing-funcs service-key))

(defn coverage
  "Convenience function that calls missing-functions [service-key]."
  [service-key]
  (missing-functions service-key))

(defn coverages
  "Convenience function that calls all-missing-functions []."
  []
  (all-missing-functions))

(defn register-namespace!
  "Registers a service namespace given a :service-key, :service-seed-ns string namespace, :service-func-names list of function names as keywords, and
   optional :force? boolean (defaults to false) which will thrown an exception if one function is not defined in the namespace.
   If successful, returns the entire @service-namespaces atom map.

  Example:
    (register-namespace! :storage :prj-name.store.mongo [:create :retrieve :update :delete] false)
    ;; register service mapping for service-key :storage and namespace :prj-name.store.mongo with service function names [:create :retrieve :update :delete]
    ;; and do not force? (false) to register if there are any functions in the function names list [:create :retrieve :update :delete] not resolvable
    ;; in the given namespace (:prj-name.store.mongo)."
  [service-key service-seed-ns service-func-names & [force?]]
  (let [res (service-ns-map service-seed-ns service-func-names force?)]
    (swap! service-namespaces assoc service-key res)))

(defn unregister-namespace!
  "Unregisters service namespace given the service key :service-key."
  [service-key]
  (swap! service-namespaces dissoc service-key))

(defn set-namespace!
  "Sets a service namespace given an existing :service-key, :service-seed-ns string namespace, :service-func-names list of function names as keywords, and
   optional :force? boolean (defaults to false) which will thrown an exception if one function is not defined in the namespace."
  [service-key service-ns & [force?]]
  (let [sns (@service-namespaces service-key)]
    (if sns
      (do
        (swap! service-namespaces assoc service-key (service-ns-map service-ns (:func-names sns) force?))
        (service-key @service-namespaces)))))

(defn get-namespace
  "Returns the mapped namespace of the service-key parameter, nil if no mapping
  for the provide service-key."
  [service-key]
  (:ns (@service-namespaces service-key)))

(defn get-namespaces
  "Returns a mapping of all service-keys with the service-key as the key and the mapped namespace
  as the value."
  []
  (reduce #(assoc % %2 (:ns (@service-namespaces %2))) nil (keys @service-namespaces)))

(defn call
  "Calls service function for a given :service-key and :fkey function key, given the :args list of arguments."
  [service-key fkey & args]
  (let [f (-> @service-namespaces service-key :funcs fkey)
        lns (-> @service-namespaces service-key :ns)]
    (if f
      (apply f args)
      (throw (RuntimeException. (str "Exception in " service-key " service call occurred due to no function '"
                              (name fkey) "' definition in namespace '" lns "' (" (str lns "/" (name fkey)) ") with "
                              (if (> (count args) 0) (str "arguments: " args) "no arguments")  "."))))))

