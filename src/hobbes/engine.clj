(ns hobbes.engine
  "Engine used to parse .hob input and return a parse tree for 
  further processing."
  (:require [instaparse.core :as insta]
            [hobbes.utils :refer :all]))


(def ^:private grammar-specifications
  "File instance containing the grammar specifications of the Hobbes language."
  (clojure.java.io/resource "parser2.bnf"))

(def ^:private parser
  "Loads the parser specifications for .hob files and sends it to Instaparse
  parser generator.
  Returns the parser instance."
  (insta/parser grammar-specifications
                :output-format :enlive))

(def ^:private block-transforms
  "Instaparse AST transformations to apply to blocks.
  Returns an AST of valid enlive blocks, with valid HTML5 tags."
  (let [concat-c (fn [tag c] {:tag tag :content (apply str c)})]
    {:HEADER     (fn [{:keys [tag]} & c] (concat-c (lower-keyword tag) c))
     :PARAGRAPH  (fn [& c] (concat-c :p c))
     :TPARAGRAPH (fn [& c] (concat-c :p c))
     :RULE       (fn [& _] {:tag :hr})
     :ULIST      (fn [& c] {:tag :ul :content c})
     :OLIST      (fn [& c] {:tag :ol :content c})
     :ULISTLINE  (fn [& c] (concat-c :li c))
     :OLISTLINE  (fn [& c] (concat-c :li c))}))

(defn- parse-blocks
  "Parse input string block by block (ie: headers, paragraphs, lists, ...).
  Returns an instaparse AST of blocks."
  [input]
  (let [ast (insta/parse parser input)]
    (if-let [fail (insta/get-failure ast)]
      (do (println fail)
          (throw (Exception. "Ill-formed .hob file")))
      (insta/transform block-transforms ast))))

(defn- parse-spans
  "Parse an instaparse AST of blocks to delimit spans (bold, em, ...).
  Returns a final, instaparse AST containing blocks with spans."
  [input]
  input)

(defn parse
  "Parse input.
  Input must be treated beforehand; see preprocessor namespace, otherwise
  comments will remain and abbreviations/runtime variables won't be expanded.
  Returns an instaparse AST suitable for transformation."
  [input]
  (->> input
       (parse-blocks)
       (parse-spans)))
