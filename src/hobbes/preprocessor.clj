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
  #"(?s)(!\*.+\*!)")

(defn- remove-comments
  "Remove comment blocs from input (contained and rest-of-line) string.
  Returns a string."
  [input]
  (-> input
      (s/replace comment-pattern "")
      (s/replace contained-comment-pattern "")))

(defn- remove-whitespaces
  "Takes a string as input and remove trailing and leading whitespaces on each
  line, as well as consecutive whitespaces between words."
  [input]
  (s/replace (s/join "\n" (map s/trim (s/split-lines input))) #" {2,}" " "))

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

(def ^:private default-abbrevs
  "Default map of abbreviations."
  (try
    (let [lang (System/getProperty "user.language")]
      (parse-props (clojure.java.io/resource
                    (str "default/abbreviations_" lang ".properties"))))
    (catch Exception e
      {})))

(def ^:private default-variables
  "Default map of runtime variables."
  (try
    (let [lang (System/getProperty "user.language")]
      (into {} (eval-map-resource (str "default/variables_" lang ".edn"))))
    (catch Exception e
      {})))


; Public API
(defn preprocess
  "Preprocess string input, making it suitable to parse.
  Returns the treated input as a string."
  [input & [abbr-map variables-map]]
  (let [abbrs (merge default-abbrevs abbr-map)
        vars  (merge default-variables variables-map)]
    (->> input
         (s/trim)
         (remove-comments)
         (remove-whitespaces)
         (expand-abbrevs abbrs)
         (expand-runtime-variables vars)
         (add-padding-newlines))))
