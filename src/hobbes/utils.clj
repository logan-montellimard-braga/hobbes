(ns hobbes.utils
  "Utility functions."
  (:require [clojure.string :as s]))

;;;
; Seqs utilities
;;;
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

(defn flatten-if-seq
  "Takes a seq or a non-seq elem, and flattens it if it is a seq, otherwise
  returns the original input."
  [input]
  (if (seq? input)
    (flatten input)
    input))

(defn find-first
  "Takes a function and a coll and returns the first element of coll for
  which f returns true. Otherwise, returns nil."
  [f coll]
  (first (filter f coll)))

;;;
; Keywords and symbols utilities
;;;
(defn lower-keyword
  "Takes a keyword as input and lowercase it, converting it back to keyword.
  Input may also be a string.
  Returns a keyword."
  [k]
  (->> k
       (name)
       (s/lower-case)
       (keyword)))

;;;
; File utilities
;;;
(defn parse-props
  "Takes a jproperties file as input and tries to parse its content into a map."
  [file]
  (let [props (java.util.Properties.)]
    (.load props (clojure.java.io/reader file))
    (into {} props)))
