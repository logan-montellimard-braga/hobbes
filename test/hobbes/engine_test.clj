(ns hobbes.engine-test
  (:require [clojure.test :refer :all]
            [hobbes.engine :refer :all]))

; Note : these tests do NOT depend on hobbes.preprocessor that is
; normally necessary before parsing. To counter that, we add \n chars
; at the end of inputs and do not test input with comments. Such cases should
; be covered in hobbes.preprocessor tests.

(deftest parse-blocks-test
  (testing "Ill-formed input"
    (binding [*out* nil]
      (is (thrown? java.lang.Exception (#'hobbes.engine/parse-blocks "a")))))
  (testing "Correct, simple input"
    (is (= '({:tag :HEADER, :content
              ({:tag :H1, :content nil} "foo" {:tag :EOL, :content nil})})
           (#'hobbes.engine/parse-blocks "_t foo\n"))))
  (testing "Flexibility of special tokens"
    (is (= '({:tag :HEADER, :content
              ({:tag :H2, :content nil} "foo" {:tag :EOL, :content nil})})
           (#'hobbes.engine/parse-blocks "_stit foo\n")))))

(deftest parse-spans-test
  (testing "String without spans to detect"
    (is (= "foo"
           (#'hobbes.engine/parse-spans "foo"))))
  (testing "String with 'false', non-complete span"
    (is (= "foo*bar"
           (#'hobbes.engine/parse-spans "foo*bar"))))
  (testing "String with one span to detect"
    (is (= '("foo " {:tag :strong, :content "bold", :attrs nil} " not bold")
           (#'hobbes.engine/parse-spans "foo *bold* not bold"))))
  (testing "String with several spans to detect"
    (is (= '("foo " {:tag :strong, :content "bold", :attrs nil}
                    (" " {:tag :em, :content "italic", :attrs nil} " normal"))
           (#'hobbes.engine/parse-spans "foo *bold* /italic/ normal"))))
  (testing "String with nested spans"
    (is (= '("" {:tag :strong
                 :content ("bold" {:tag :u, :content "underline", :attrs nil}
                                  ""), :attrs nil} "")
           (#'hobbes.engine/parse-spans "*bold_underline_*"))))
  (testing "Special case: link span"
    (is (= '("here a " {:tag :a, :content "Link",
                        :attrs {:href "link.html" :class "internal"}} "")
           (#'hobbes.engine/parse-spans "here a ->link")))))

(deftest split-at-blocks-test
  (testing "Input shouldn't be split"
    (is (= '("foo")
           (#'hobbes.engine/split-at-blocks "foo"))))
  (testing "Input should be split"
    (is (= '("foo\n\n" "_t title\nbar")
           (#'hobbes.engine/split-at-blocks "foo\n\n_t title\nbar")))))

(deftest parse-test
  (testing "Empty input"
    (is (= '()
           (parse ""))))
  (testing "Real input"
    (is (= '({:tag :h1, :content "My title", :attrs {:id "my_title"}}
             {:tag :p, :content ("my paragraph in "
                                 {:tag :strong, :content "bold", :attrs nil}
                                 "")})
           (parse "_Ti My title\nmy paragraph in *bold*\n\n")))))
