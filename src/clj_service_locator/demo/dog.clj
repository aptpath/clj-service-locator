(ns clj-service-locator.demo.dog)

(defn greet
  "Service implementation of :greet function."
  ([] (greet nil))
  ([n] (str "Arf" (if n (str ", " n) "") ".")))

(defn scientific-name
  "Service implementation of :scientific-name function."
  []
  "Canis lupus familiaris")
