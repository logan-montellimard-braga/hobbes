(ns hobbes.generator-test
  (:require [clojure.test :refer :all]
            [hobbes.generator :refer :all]))

(deftest get-tmpl-name-test
  (testing "Incorrect name path"
    (is (= nil
           (#'hobbes.generator/get-tmpl-name "kdjskdz"))))
  (testing "Correct name path (contained in default resource)"
    (is (= (clojure.java.io/resource "default/template/layout.html")
           (#'hobbes.generator/get-tmpl-name "layout")))))
