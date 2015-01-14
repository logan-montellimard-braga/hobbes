(ns hobbes.preprocessor-test
  (:require [clojure.test :refer :all]
            [hobbes.preprocessor :refer :all]))

(deftest remove-comments
  (testing "Cleaning comments from string input"
    (is (= []
           (#'hobbes.preprocessor/remove-comments "!! Commented !"))))
  (testing "Cleaning comments from coll input"
    (is (= ["Not comment"]
           (#'hobbes.preprocessor/remove-comments '("Not comment" "!! com" "!! too")))))
  (testing "Cleaning comments in string input without comments"
    (is (= ["Untouched."]
           (#'hobbes.preprocessor/remove-comments "Untouched.")))))

(deftest adding-padding
  (testing "Input has no padding newlines."
    (is (= "Not yet\n\n"
           (#'hobbes.preprocessor/add-padding-newlines "Not yet")))))


(deftest putting-it-all-together
  (testing "Preprocessing multiline input."
    (is (= "Trimmed,\nno comments,\npadding.\n\n"
           (preprocess "  \tTrimmed,\n!!comment\nno comments,\npadding.")))))
