(ns hobbes.engine
  "Engine used to parse .hob input and return a parse tree for 
  further processing."
  (:require [instaparse.core :as insta]
            [hobbes.utils :refer :all]))


(def ^:private grammar-specifications
  "File instance containing the grammar specifications of the Hobbes language."
  (clojure.java.io/resource "parser.bnf"))

(def ^:private parser
  "Loads the parser specifications for .hob files and sends it to Instaparse
  parser generator.
  Returns the parser instance."
  (insta/parser grammar-specifications
                :output-format :enlive))

(def ^:private span-regexs
  "Vectors of [t, r, a] containing all possible inline element,
  where t is the tag of the element, r is the regex to extract it
  and a is a function returning the map of attributes to give to the element
  once parsed (use of functions instead of direct map to allow for closures
  from the parser)."
  (let [null-f (fn [& _] nil)]
    [[:a    #"(.*)\s->(\S+)<-(.*)"  (fn [l]   {:href l :class "external"})]
     [:a    #"(.*)\s->(\S+)(.*)"    (fn [l]   {:href (str l ".html") :class "internal"})]
     [:b    #"(.*)\*([^\*]+)\*(.*)" null-f]
     [:i    #"(.*)/([^/]+)/(.*)"    null-f]
     [:u    #"(.*)_([^_]+)_(.*)"    null-f]
     [:img  #"(.*)\s(\S+(?:\.(?:png|jpe?g|gif|bmp)))($|\s.*)" (fn [i] {:src i :alt i})]
     [:span #"(.*)--([^--]+)--(.*)" (fn [& _] {:class "striked"})]
     [:span #"(.*)#\?\?+()(.*)"     (fn [& _] {:class "missing"})]
     [:span #"(?i)(.*)\(ex(?:e(?:m(?:p(?:l(?:e)?)?)?)?)?\s*:\s*(.+)\)(.*)" (fn [& _] {:class "example"})]
     [:code #"(.*)<([^>]+)>(.*)"    null-f]]))

(defn- parse-spans
  "Parse a string to delimit spans (bold, em, ...).
  Returns a constructed map output in enlive format containing spans."
  [input]
  (if-let [span (find-first identity (for [[tag re attrs] span-regexs]
                                       (if-let [match (re-matches re input)]
                                         (conj match tag attrs))))]
    (let [[_ before c after tag attrs] span]
      (list (parse-spans before)
            {:tag tag
             :content (case tag
                        :a    c
                        :img  nil
                        :code c
                        (parse-spans c))
             :attrs (attrs c)}
            (parse-spans after)))
    input))

(def ^:private block-transforms
  "Instaparse AST transformations to apply to blocks.
  Returns an AST of valid enlive blocks, with valid HTML5 tags."
  (let [concat-parse (fn [s] (->> s
                                  (apply str)
                                  (parse-spans)
                                  (flatten-if-seq)))
        concat-c (fn [tag content] {:tag tag :content (concat-parse content)})]
    {:HEADER     (fn [{:keys [tag]} & c] (concat-c (lower-keyword tag) c))
     :QUOTE      (fn [{:keys [content]} & c] {:tag :blockquote
                                              :content (flatten
                                                         (list (concat-parse c)
                                                               {:tag :cite
                                                                :content (concat-parse content)}))})
     :DEF        (fn [{:keys [content]} & c] {:tag :div
                                              :attrs {:class "definition"}
                                              :content {:tag :p
                                                        :content (flatten
                                                         (list {:tag :span
                                                                :attrs {:class "deftitle"}
                                                                :content (concat-parse content)}
                                                               (concat-parse c)))}})
     :PARAGRAPH  (fn [& c] (concat-c :p c))
     :TPARAGRAPH (fn [& c] (concat-c :p c))
     :RULE       (fn [& _] {:tag :hr})
     :ULIST      (fn [& c] {:tag :ul :content c})
     :OLIST      (fn [& c] {:tag :ol :content c})
     :ULISTLINE  (fn [& c] (concat-c :li c))
     :OLISTLINE  (fn [& c] (concat-c :li c))}))

(defn- parse-blocks
  "Parse input string block by block (ie: headers, paragraphs, lists, ...).
  May throw exception if input is incorrect and fails to parse.
  Returns an instaparse AST of blocks."
  [input]
  (let [ast (insta/parse parser input)]
    (if-let [fail (insta/get-failure ast)]
      (do (println fail)
          (throw (Exception. "Ill-formed .hob file")))
      ast)))

(defn parse
  "Parse input.
  Input must be treated beforehand; see preprocessor namespace, otherwise
  comments will remain and abbreviations/runtime variables won't be expanded.
  Returns an instaparse AST suitable for transformation."
  [input]
  (->> input
       (parse-blocks)
       (insta/transform block-transforms)))
