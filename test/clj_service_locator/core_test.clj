(ns clj-service-locator.core-test
  (:require [clojure.test :refer :all]
            [clj-service-locator.core :refer :all]
            [clj-service-locator.demo.facade :as facade]))

(deftest human-test
  (testing "Human..."
    (facade/set-namespace! :clj-service-locator.demo.human)
    (is (.equals "Homo sapien" (facade/scientific-name)))
    (is (.equals "Hi." (facade/greet)))
    (is (.equals "Hi." (facade/hello)) "Called function in the service facade namespace does not have to be same in the service namespace.")
    (is (.equals "Hi, Jake." (facade/greet "Jake")))))

(deftest dog-test
  (testing "Dog..."
    (facade/set-namespace! "clj-service-locator.demo.dog")
    (is (.equals "Canis lupus familiaris" (facade/scientific-name)))
    (is (.equals "Arf." (facade/greet)))
    (is (.equals "Arf, Jake." (facade/greet "Jake")))))

(deftest cat-test
  (testing "Cat..."
    (is (thrown? RuntimeException (facade/set-namespace! "clj-service-locator.demo.cat")) "Should throw an exception is no force? flag and missing functions.")
    (facade/set-namespace! "clj-service-locator.demo.cat" true)
    (is (thrown? RuntimeException (.equals "Felinis non get here-is" (facade/scientific-name))) "Should thrown an exception if call missing function.")
    (is (.equals "Meow." (facade/greet)))
    (is (.equals "Meow, Jake." (facade/greet "Jake")))))

(deftest missing-funcs-test
  (testing "Missing functions..."
    (facade/set-namespace! "clj-service-locator.demo.cat" true)
    (is (= [:scientific-name] (:missing-funcs (missing-functions :dispatcher))))))

(deftest get-namespace-mapping-test
  (testing "Mapping of service keys to namespace"
    (facade/set-namespace! "clj-service-locator.demo.dog" true)
    (is (.equals "clj-service-locator.demo.dog" (get-namespace :dispatcher)))
    (facade/set-namespace! "clj-service-locator.demo.cat" true)
    (is (.equals "clj-service-locator.demo.cat" (get-namespace :dispatcher)))))

(deftest get-namespace-mappings-test
  (testing "Mappings of service keys to namespace"
    (facade/set-namespace! "clj-service-locator.demo.dog" true)
    (is (.equals {:dispatcher "clj-service-locator.demo.dog"} (get-namespaces)))
    (register-namespace! :foo "clj-service-locator.demo.human" [:greet])
    (is (.equals {:dispatcher "clj-service-locator.demo.dog" :foo "clj-service-locator.demo.human"}
                 (get-namespaces)))
    (unregister-namespace! :foo)
    (is (.equals {:dispatcher "clj-service-locator.demo.dog"}
                 (get-namespaces)))))


