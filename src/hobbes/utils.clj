(ns hobbes.utils
  "Utility functions."
  (:require [clojure.string :as s]
            [clojure.java.io :as io]))

(defn name-or-re
  "If input is a regex, returns it. Otherwise, returns (name input)."
  [input]
  (cond
    (= java.util.regex.Pattern (class input)) input
    (= clojure.lang.PersistentList (class input)) (eval input)
    :else (str (name input) " ")))

(defn map-replace
  "Takes a map of {k v} and a string and replaces every occurence of k with v
  in the string, for each k, v in the map. k and v may be keywords, strings,
  or regexs.
  Returns the treated string."
  ([string] string)
  ([m string]
   (reduce
     (fn [acc [k v]] (s/replace acc (name-or-re k) (name-or-re v)))
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
; Keywords, symbols and strings utilities
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

(defn get-domain-name
  "Takes a string as input and tries to parse it as a URL. If it succeeds,
  returns the domain name minus 'www.' if it was present. If it fails, returns
  the capitalized, original input."
  [url]
  (try
    (if-let [host (.getHost (java.net.URI. url))]
      (if (.startsWith host "www.")
        (subs host 4)
        host)
      (s/capitalize url))
    (catch Exception e
      url)))

(defn dasherize
  "Takes a string as input and lowercases it, converting all spaces to dashes.
  Returns a string."
  [s]
  (s/lower-case (s/replace s #"\s" "_")))

(defn trim*
  "Takes a collection of strings as input and concats them, then trims the
  result.
  Returns a string."
  [string]
  (s/trim (apply str string)))

;;;
; File utilities
;;;
(defn parse-props
  "Takes a jproperties file as input and tries to parse its content into a map."
  [file]
  (let [props (java.util.Properties.)]
    (try
      (.load props (io/reader file))
      (catch java.io.FileNotFoundException e))
    (into {} props)))

(defn eval-map-resource
  "Evals the content of given resource file, and returns it."
  [file]
  (read-string (slurp (io/resource file))))
