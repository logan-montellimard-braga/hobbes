(ns hobbes.preprocessor
  "Engine used to preprocess input string for optimal parsing."
  (:require [clojure.string :as s]))

; Treat input formatting
(def ^:private comment-pattern #"^\s*!!.*")

(defn- remove-comments
  "Remove comment lines from input, be it a string or a coll of strings.
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


; Expand abbreviations
(defn map-replace
  "Takes a map of {k v} and a string and replaces every occurence of k with v
  in the string, for each k, v in the map. k and v may be keywords or strings.
  Returns the treated string."
  ([string] string)
  ([m string]
   (reduce
     (fn [acc [k v]] (s/replace acc (name k) (name v)))
     string m)))

(defn add-prefix-to-map-keys
  "Takes a map and a prefix string, and adds the prefix to each key of the map,
  converting them to strings.
  Returns the treated map."
  ([m] m)
  ([m prefix]
  (into (empty m)
        (for [[k v] m]
          [(str prefix (name k)) v]))))

(defn- expand-abbrevs
  "Take a string input and a map of abbreviations, and expands all of them.
  Returns the treated string input."
  [string abbrs]
  (-> abbrs
       (add-prefix-to-map-keys "~")
       (map-replace string)))


; Public API
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
