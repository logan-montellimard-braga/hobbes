(ns hobbes.preprocessor-test
  (:require [clojure.test :refer :all]
            [hobbes.preprocessor :refer :all]))

(deftest removing-comments
  (testing "Cleaning comments from monoline input"
    (is (= ""
           (#'hobbes.preprocessor/remove-comments "!! Commented !"))))
  (testing "Cleaning comments in input without comments"
    (is (= "Untouched."
           (#'hobbes.preprocessor/remove-comments "Untouched."))))
  (testing "Cleaning comments from multiline input"
    (is (= "Not commented.\n here neither"
           (#'hobbes.preprocessor/remove-comments "Not commented.\n!! commented\n here neither")))))

(deftest triming-lines
  (testing "Trimming whitespaces"
    (is (= "Perfectly trimmed.\nHere too."
           (#'hobbes.preprocessor/trim-each-line "   Perfectly trimmed.  \n Here too.  "))))
  (testing "Trimming tabs and feeds"
    (is (= "Perfectly trimmed.\nHere too."
           (#'hobbes.preprocessor/trim-each-line "\tPerfectly trimmed.\n\tHere too.\f")))))

(deftest adding-padding
  (testing "Input already has two padding newlines."
    (is (= "Already there\n\n"
           (#'hobbes.preprocessor/add-padding-newlines "Already there\n\n"))))
  (testing "Input has one padding newline."
    (is (= "One is here\n\n"
           (#'hobbes.preprocessor/add-padding-newlines "One is here\n"))))
  (testing "Input already has no padding newlines."
    (is (= "Not yet\n\n"
           (#'hobbes.preprocessor/add-padding-newlines "Not yet")))))


(deftest putting-it-all-together
  (testing "Preprocessing multiline input."
    (is (= "Trimmed,\nno comments,\npadding.\n\n"
           (preprocess "  \tTrimmed,\n!!comment\nno comments,\npadding.")))))
