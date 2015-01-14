(ns hobbes.preprocessor
  "Engine used to preprocess input string for optimal parsing."
  (:require [clojure.string :as s]))

(def ^:private comment-pattern #"^\s*!!.*")

(defn- remove-comments
  "Remove comment lines from input, be it a string or
  a coll of strings.
  Returns a coll."
  [input]
  (let [coll (if (coll? input) input (vector input))]
    (remove #(re-matches comment-pattern %) coll)))

(defn- add-padding-newlines
  "Add newlines to end of string if needed, to correctly treat last element,
  because special elements at end of input (ie: not paragraphs) are treated as 
  paragraphs by the parser if there is no 2 trailing newlines after them.
  Returns the treated input."
  [input]
  (str input "\n\n"))

(defn preprocess
  "Preprocess input, making it suitable to parse.
  Returns the treated input as string."
  [input]
  (->> input
       (s/split-lines)
       (remove-comments)
       (map s/trim)
       (s/join "\n")
       (add-padding-newlines)))
