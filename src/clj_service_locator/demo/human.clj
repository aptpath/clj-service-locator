(ns clj-service-locator.demo.human)

(defn greet
  "Service implementation of :greet function."
  [& [n]]
  (str "Hi" (if n (str ", " n) "") "."))

(defn scientific-name
  "Service implementation of :scientific-name function."
  []
  "Homo sapien")
