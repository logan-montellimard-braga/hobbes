(ns hobbes.utils
  "Utility functions."
  (:require [clojure.string  :as s]
            [clojure.java.io :as io]
            [me.raynes.fs    :as f]
            [clojure.walk    :as w]
            [clojure.edn     :as edn])
  (:import  [java.util.jar JarFile JarEntry]))

(defn name-or-re
  "If input is a regex, returns it. Otherwise, returns (name input)."
  [input]
  (cond
    (= java.util.regex.Pattern (class input)) input
    (= clojure.lang.PersistentList (class input)) (str (eval input) " ")
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

(defn pmapcat
  "mapcat with parallelism; fusion of mapcat and pmap.
  Takes a function f and a coll as input, and concurrently applies f to each
  item in coll, then concats the result. Order of items is conserved.
  Returns a coll."
  [f coll]
  (apply concat (pmap f coll)))

(defn extract-strings
  "Takes a coll and returns a string composed of the concatenation of all
  the strings in coll, recursively."
  [coll]
  (let [strs (atom [])]
    (w/postwalk #(if (= java.lang.String (class %))
                              (swap! strs conj %)) coll)
    (clojure.string/join " " @strs)))

(defn count-words
  "Takes a string and returns the number of words, as separated by spaces."
  [string]
  (count (s/split string #"\s")))

(defn compute-estimated-reading-time
  "Takes a string and a words-per-minute number and calculates estimated
  reading time for this string.
  Returns a map of the form {:unit value}."
  [string wpm]
  (let [words   (count-words string)
        minutes (Math/floor (/ words wpm))
        seconds (Math/floor (/ (mod words wpm) (/ wpm 60)))]
    {:m (str (int minutes)) :s (str (int seconds))}))

;;;
; Keywords, symbols and strings utilities
;;;
(defn lower-keyword
  "Takes a keyword as input and lowercase it, converting it back to keyword.
  Input may also be a string.
  Returns a keyword."
  [k]
  (->> k name s/lower-case keyword))

(defn filename->title
  "Takes a string representing a typical file name (eg: snake_case or camelCase)
  and returns a title-ized version with separated, capitalized words.
  Capitalization rules may not suit all locales."
  [string]
  (s/join " " (remove s/blank? (map #(-> % s/trim s/capitalize)
                                    (s/split (s/replace string "_" " ")
                                             #"(?=[A-Z\s])")))))

(defn get-domain-name
  "Takes a string as input and tries to parse it as a URL. If it succeeds,
  returns the domain name minus 'www.' if it was present. If it fails, returns
  the input as an hob title."
  [url]
  (try
    (if-let [host (.getHost (java.net.URI. url))]
      (if (.startsWith host "www.")
        (subs host 4)
        host)
      (filename->title url))
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
  (s/trim (s/join string)))

(defn date-now
  "Returns the current date as string in format day-month-year.
  May take an optional format string as argument to customize the formatting.
  Note: only a dummy wrapper, nothing fancy here."
  [& [format]]
  (let [now (java.util.Date.)
        fmt (or format "dd-MM-yyyy")]
    (.format (java.text.SimpleDateFormat. fmt) now)))

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

(defn eval-map
  "Evals the content of given edn file, and returns it."
  [file]
  (try
    (edn/read-string (slurp file))
    (catch Exception e {})))

(defn eval-map-resource
  "Evals the content of given resource edn file, and returns it."
  [file]
  (eval-map (io/resource file)))

(defn get-running-jar
  "Get the path of the running jar."
  []
  (-> (class *ns*)
      .getProtectionDomain .getCodeSource .getLocation .getPath))

(defn extract-dir-from-jar
  "Takes the string path of a jar, a dir name inside that jar and a destination
  dir, and copies the from dir to the to dir."
  [^String jar-dir from to]
  (let [jar (JarFile. jar-dir)]
    (doseq [^JarEntry file (filter (fn [^JarEntry e]
                                    (re-matches from (.getName e)))
                                   (enumeration-seq (.entries jar)))]
      (let [f (f/file to (.getName file))]
        (if (.isDirectory file)
          (f/mkdir f)
          (do (f/mkdirs (f/parent f))
              (with-open [is (.getInputStream jar file)
                          os (io/output-stream f)]
                (io/copy is os))))))))
