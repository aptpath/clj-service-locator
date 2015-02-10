(ns clj-service-locator.core-test
  (:require [clojure.test :refer :all]
            [clj-service-locator.core :refer :all]
            [clj-service-locator.demo.facade :as facade]))

(def cat-ns "clj-service-locator.demo.cat")
(def dog-ns "clj-service-locator.demo.dog")
(def human-ns "clj-service-locator.demo.human")

(deftest human-test
  (testing "Human..."
    (facade/set-namespace! human-ns)
    (is (.equals "Homo sapien" (facade/scientific-name)))
    (is (.equals "Hi." (facade/greet)))
    (is (.equals "Hi." (facade/hello)) "Called function in the service facade namespace does not have to be same in the service namespace.")
    (is (.equals "Hi, Jake." (facade/greet "Jake")))))

(deftest dog-test
  (testing "Dog..."
    (facade/set-namespace! dog-ns)
    (is (.equals "Canis lupus familiaris" (facade/scientific-name)))
    (is (.equals "Arf." (facade/greet)))
    (is (.equals "Arf, Jake." (facade/greet "Jake")))))

(deftest cat-test
  (testing "Cat..."
    (is (thrown? RuntimeException (facade/set-namespace! cat-ns)) "Should throw an exception is no force? flag and missing functions.")
    (facade/set-namespace! cat-ns true)
    (is (thrown? RuntimeException (.equals "Felinis non get here-is" (facade/scientific-name))) "Should thrown an exception if call missing function.")
    (is (.equals "Meow." (facade/greet)))
    (is (.equals "Meow, Jake." (facade/greet "Jake")))))

(deftest missing-funcs-test
  (testing "Missing functions..."
    (facade/set-namespace! cat-ns true)
    (is (= [:scientific-name] (:missing-funcs (missing-functions :facade))))))

(deftest missing-funcs-core-register-test 
  (testing "Missing functions in core..."
    (register-namespace! :xyz human-ns [:greet :scientific-name])
    (is (.equals "Hi." (call :xyz :greet)))
    (is (thrown? RuntimeException (set-namespace! :xyz cat-ns)) "Should throw an exception is no force? flag and missing functions.")
    (is (thrown? RuntimeException (register-namespace! :xyz cat-ns [:greet :scientific-name])) "Should throw an exception is no force? flag and missing functions.")
    (is (thrown? RuntimeException (register-namespace! :xyz cat-ns [:greet :scientific-name] nil)) "Should throw an exception is no force? flag and missing functions.")
    (is (thrown? RuntimeException (register-namespace! :xyz cat-ns [:greet :scientific-name] false)) "Should throw an exception is no force? flag and missing functions.")
    (register-namespace! :xyz cat-ns [:greet :scientific-name] true)
    (is (.equals "Meow." (call :xyz :greet)))
    (unregister-namespace! :xyz)
    (is (thrown? RuntimeException (call :xyz :greet)) "Should throw an exception since :xyz is unregistered")
    (register-namespace! :xyz cat-ns [:greet :scientific-name] 6.28)
    (is (.equals "Meow." (call :xyz :greet)))))

(deftest get-namespace-mapping-test
  (testing "Mapping of service keys to namespace"
    (facade/set-namespace! dog-ns true)
    (is (.equals dog-ns (get-namespace :facade)))
    (facade/set-namespace! cat-ns true)
    (is (.equals cat-ns (get-namespace :facade)))))

(deftest get-namespace-mappings-test
  (testing "Mappings of service keys to namespace"
    (facade/set-namespace! dog-ns true)
    (is (.equals {:facade dog-ns} (get-namespaces)))
    (register-namespace! :foo human-ns [:greet])
    (is (.equals {:facade dog-ns :foo human-ns}
                 (get-namespaces)))
    (unregister-namespace! :foo)
    (is (.equals {:facade dog-ns}
                 (get-namespaces)))))


