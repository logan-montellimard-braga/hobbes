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

(deftest add-padding-newlines
  (testing "Input has no padding newlines."
    (is (= "Not yet\n\n"
           (#'hobbes.preprocessor/add-padding-newlines "Not yet")))))

(deftest expand-abbrevs
  (testing "Input has no abbreviation pattern and no abbr map."
    (is (= "Nothing changed."
           (#'hobbes.preprocessor/expand-abbrevs {} "Nothing changed."))))
  (testing "Input has no abbreviation pattern but an abbr map."
    (is (= "Nothing changed."
           (#'hobbes.preprocessor/expand-abbrevs {:Nothing :Something}
                                                 "Nothing changed."))))
  (testing "Input has abbreviation pattern but no abbr map."
    (is (= "~Nothing changed."
           (#'hobbes.preprocessor/expand-abbrevs {}
                                                 "~Nothing changed."))))
  (testing "Input has abbreviation pattern and abbr map."
    (is (= "Something changed."
           (#'hobbes.preprocessor/expand-abbrevs {:Nothing :Something}
                                                 "~Nothing changed.")))))


(deftest putting-it-all-together
  (testing "Preprocessing multiline input."
    (is (= "Trimmed,\nno comments,\nnew-padding.\n\n"
           (preprocess "  \tTrimmed,\n!!comment\nno comments,\n~padding."
                       {:padding :new-padding})))))
