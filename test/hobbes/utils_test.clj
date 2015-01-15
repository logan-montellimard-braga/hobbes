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
