(ns hobbes.preprocessor
  "Engine used to preprocess input string for optimal parsing."
  (:require [clojure.string :as s]
            [hobbes.utils :refer :all]))


; Treat input formatting
(def ^:private comment-pattern
  "Regex to match a rest-of-line comment, eg. 'Not commented !! commented'"
  #"(?m)!!.+$")

(def ^:private contained-comment-pattern
  "Regex to match contained comment, be it mono or multiline.
  Everything between !* and *! is treated as a comment,
  eg. 'Not commented !* commented *!.'"
  #"(!\*.+\*!)")

(defn- remove-comments
  "Remove comment blocs from input (contained and rest-of-line) string.
  Returns a string."
  [input]
  (-> input
      (s/replace comment-pattern "")
      (s/replace contained-comment-pattern "")))

; Has own method for now cause implementation is really subject to change
; Soon to be deprecated, certainly
(defn- add-padding-newlines
  "Add newlines to end of string if needed, to correctly treat last element,
  because special elements at end of input (ie: not paragraphs) are treated as 
  paragraphs by the parser if there is no 2 trailing newlines after them.
  Returns the treated input."
  [input]
  (str input "\n"))


; Expand abbreviations and runtime variables
(defn- expand-map-in-str
  "Take a prefix, a string input and a map of k v, and replace tokens
  in input equal to <prefix>k with corresponding v.
  Returns the treated string input."
  [prefix m string]
  (-> m
      (add-prefix-to-map-keys prefix)
      (map-replace string)))

(def ^:private expand-abbrevs
  "Abbreviations start with a ~ (tilde)."
  (partial expand-map-in-str "~"))

(def ^:private expand-runtime-variables
  "Runtime variables start with a =."
  (partial expand-map-in-str "="))


; Public API
(defn preprocess
  "Preprocess string input, making it suitable to parse.
  Returns the treated input as a string."
  [input & [abbr-map variables-map]]
  (let [abbrs (or abbr-map {})
        vars  (or variables-map {})]
    (->> input
         (s/trim)
         (remove-comments)
         (s/split-lines)
         (map s/trim)
         (s/join "\n")
         (expand-abbrevs abbrs)
         (expand-runtime-variables vars)
         (add-padding-newlines))))
