(ns hobbes.preprocessor-test
  (:require [clojure.test :refer :all]
            [hobbes.preprocessor :refer :all]))

(deftest remove-comments-test
  (testing "Cleaning comments from string input"
    (is (= ""
           (#'hobbes.preprocessor/remove-comments "!! Commented !"))))
  (testing "Cleaning inline comment from string input"
    (is (= "Hey "
           (#'hobbes.preprocessor/remove-comments "Hey !* Commented *!"))))
  (testing "Cleaning comments in string input without comments"
    (is (= "Untouched."
           (#'hobbes.preprocessor/remove-comments "Untouched.")))))

(deftest remove-whitespaces-test
  (testing "No cleaning needed"
    (is (= "foo"
           (#'hobbes.preprocessor/remove-whitespaces "foo"))))
  (testing "Removing trailing and leading whitespaces"
    (is (= "foo"
           (#'hobbes.preprocessor/remove-whitespaces "\t  \ffoo \t"))))
  (testing "Removing consecutive spaces between words"
    (is (= "foo bar"
           (#'hobbes.preprocessor/remove-whitespaces "foo   bar")))))

(deftest add-padding-newlines-test
  (testing "Input has no padding newlines."
    (is (= "Not yet\n"
           (#'hobbes.preprocessor/add-padding-newlines "Not yet")))))

(deftest expand-abbrevs-test ; same for expand-runtime-variables
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
                                                 "~Nothing changed."))))
  (testing "Input has abbreviation pattern and abbr map with several keys."
    (is (= "Something happened "
           (#'hobbes.preprocessor/expand-abbrevs {:Nothing :Something
                                                  :changed :happened}
                                                 "~Nothing ~changed ")))))


(deftest preprocess-test
  (testing "Preprocessing multiline input."
    (is (= "Trimmed,\nno comments,\nnew-padding .\n"
           (preprocess "  \tTrimmed,!!comment\nno    comments,\n~padding ."
                       {:padding :new-padding} {})))))
