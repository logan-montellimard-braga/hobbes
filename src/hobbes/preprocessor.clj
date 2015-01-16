(ns hobbes.preprocessor
  "Engine used to preprocess input string for optimal parsing."
  (:require [clojure.string :as s]
            [hobbes.utils :refer :all]))


; Treat input formatting
(def ^:private comment-pattern #"(?m)!!.*$")
(def ^:private inline-comment-pattern #"(!\*.+\*!)")

(defn- remove-comments
  "Remove comment blocs from input (contained and rest-of-line) string.
  Returns a string."
  [input]
  (-> input
      (s/replace comment-pattern "")
      (s/replace inline-comment-pattern "")))

(defn- add-padding-newlines
  "Add newlines to end of string if needed, to correctly treat last element,
  because special elements at end of input (ie: not paragraphs) are treated as 
  paragraphs by the parser if there is no 2 trailing newlines after them.
  Returns the treated input."
  [input]
  (str input "\n\n"))


; Expand abbreviations and runtime variables
(defn- expand-map-in-str
  "Take a prefix, a string input and a map of k v, and replace tokens
  in input equal to <prefix>k with corresponding v.
  Returns the treated string input."
  [prefix m string]
  (-> m
      (add-prefix-to-map-keys prefix)
      (map-replace string)))

(def ^:private expand-abbrevs (partial expand-map-in-str "~"))
(def ^:private expand-runtime-variables (partial expand-map-in-str "="))


; Public API
(defn preprocess
  "Preprocess input, making it suitable to parse.
  Returns the treated input as a string."
  [input & [abbr-map variables-map]]
  (let [abbrs (or abbr-map {})
        vars  (or variables-map {})]
    (->> input
         (remove-comments)
         (expand-abbrevs abbrs)
         (expand-runtime-variables vars)
         (s/trim)
         (add-padding-newlines))))
