(ns hobbes.engine
  "Engine used to preprocess and parse .hob input and return a parse tree for 
  further processing."
  (:require [instaparse.core :as insta]
            [hobbes.preprocessor :as prep]))


(def ^:private grammar-specifications
  (clojure.java.io/resource "parser2.bnf"))

(def ^:private parser
  "Loads the parser specifications for .hob files and sends it to Instaparse
  parser generator.
  Returns the parser instance."
  (insta/parser grammar-specifications
                :output-format :hiccup))

(defn- parse-blocks
  "Parse input string block by block (ie: headers, paragraphs, lists, ...).
  Returns an instaparse AST of blocks."
  [input]
  ())

(defn- parse-spans
  "Parse an instaparse AST of blocks to delimit spans (bold, em, ...).
  Returns a final, instaparse AST containing blocks with spans."
  [input]
  ())

(defn parse
  "Parse input.
  Returns an instaparse AST suitable for transformation."
  [input]
  (->>
    input
    (prep/preprocess)
    (parse-blocks)
    (parse-spans)))
