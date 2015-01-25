(ns hobbes.utils-test
  (:require [clojure.test :refer :all]
            [hobbes.utils :refer :all]))

(deftest map-replace-test
  (testing "Empty string and empty map."
    (is (= ""
           (map-replace {} ""))))
  (testing "Input string with no map."
    (is (= "Nothing changed."
           (map-replace "Nothing changed."))))
  (testing "Input string with empty map."
    (is (= "Nothing changed."
           (map-replace {} "Nothing changed."))))
  (testing "Input string with useless map in this case."
    (is (= "Nothing changed."
           (map-replace {:foo :bar} "Nothing changed."))))
  (testing "Input string with map containing tokens of string."
    (is (= "Something changed."
           (map-replace {:Nothing :Something} "Nothing changed.")))))

(deftest add-prefix-to-map-keys-test
  (testing "Map without prefix"
    (is (= {:foo :bar}
           (add-prefix-to-map-keys {:foo :bar}))))
  (testing "Map with empty prefix."
    (is (= {"foo" :bar}
           (add-prefix-to-map-keys {:foo :bar} ""))))
  (testing "Empty map with prefix"
    (is (= {}
           (add-prefix-to-map-keys {} "foo"))))
  (testing "Map and prefix."
    (is (= {"pre-foo" :bar, "pre-baz" :quux}
           (add-prefix-to-map-keys {:foo :bar :baz :quux} "pre-")))))

(deftest lower-keyword-test
  (testing "Keyword input"
    (is (= :foo
           (lower-keyword :FoO)))))

(deftest get-domain-name-test
  (testing "Input is not a valid url"
    (is (= "Foo"
           (get-domain-name "foo"))))
  (testing "Input is a valid url"
    (is (= "google.com"
           (get-domain-name "http://www.google.com?q=query")))))

(deftest dasherize-test
  (testing "Input doesnt need to change"
    (is (= "foo"
           (dasherize "foo"))))
  (testing "Input should be dasherized"
    (is (= "foo_bar_baz"
           (dasherize "Foo Bar baz")))))

(deftest flatten-if-seq-test
  (testing "Input is a seq"
    (is (= '("foo" "bar")
           (flatten-if-seq '("foo" ("bar"))))))
  (testing "Input is not a seq"
    (is (= "foo"
           (flatten-if-seq "foo")))))

(deftest find-first-test
  (testing "Input has no wanted value"
    (is (= nil
           (find-first identity [nil false nil]))))
  (testing "Input has a wanted value"
    (is (= 1
           (find-first identity [nil false 1 nil true])))))

(deftest parse-props-test
  (testing "File does not exist"
    (is (= {}
           (parse-props "sqsKJSkkdksd")))))
