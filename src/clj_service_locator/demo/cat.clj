(ns clj-service-locator.demo.cat)

(defn greet
  "Service implementation of :greet function."
  [& [n]] 
  (str "Meow" (if n (str ", " n) "") "."))

