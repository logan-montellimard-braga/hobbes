(ns hobbes.runner
  "Hobbes runner.
  Used to launch Hobbes base actions from core namespace."
  (:require [hobbes.utils        :refer :all]
            [hobbes.preprocessor :as pre]
            [hobbes.generator    :as gen]
            [hobbes.engine       :as engine]
            [me.raynes.fs        :as f]))

(def ^:dynamic *settings*
  "Dynamic settings map. Will be populated with data parsed by hobbes.core."
  {})

(def ^:private special-files-glob
  "Dynamically construct the glob string of all special hob files."
  (memoize
   (fn [] (str "{" (clojure.string/join "," [(*settings* :abbreviations)
                                             (*settings* :variables)
                                             (*settings* :glob-format)]) "}"))))

(defn- get-mod-times
  "Get the modification times of already compiled hob files as a map from
  default location."
  [path]
  (eval-map (f/file path (*settings* :mod-times-file))))

(defn- write-mod-times
  "Sets the modification times of already compiled hob files as an edn map to
  default location."
  [path edn]
  (spit (f/file path (*settings* :mod-times-file)) (pr-str edn)))

(defn- mod-key
  "Generates the storage key inside the modtimes edn file for a given file."
  [file]
  (str (f/name (f/parent file)) "/" (f/name file)))

(defn- get-topics-dirs
  "Takes a string root path and returns a seq of all directories supposed to
  represent course topics."
  [path]
  (filter f/directory? (f/list-dir path)))

(defn- get-hob-files-in-dir
  "Takes a directory path and returns a seq of all files to treat inside it."
  [path & [glob-format]]
  (let [glob (or glob-format (*settings* :glob-format))]
    (filter f/file? (f/glob path glob))))

(defn- copy-folder-with-assets
  "Takes an input path and an output path, and a string folder name and copies
  the directory into the output dir, copying all assets and deleting hob files."
  [in out topic]
  (let [from (f/file in (f/name topic))
        to (f/file out "topics" (f/name topic))]
    (f/copy-dir-into from to)
    (dorun (map f/delete
                (get-hob-files-in-dir to (special-files-glob))))))

(defn dump-resource
  "Takes an output path and a jar-resource file/folder to extract and dumps it."
  [output resource]
  (let [from (clojure.java.io/resource resource)
        to (f/file output)
        re (re-pattern (str resource "/.+"))]
    (try
      (f/copy-dir-into from to)
      (catch Exception e
        (if-let [tmpdir (f/temp-dir "hobbes_" "_dump")]
          (do (extract-dir-from-jar (get-running-jar) re tmpdir)
              (f/copy-dir-into (f/file tmpdir resource) to)
              (f/delete-dir tmpdir))
          (throw Exception "Unable to create temp directory."))))))

(def ^:private tmpl-location
  "User-defined names and relative paths."
  {:layout "layout.html"
   :index  "index.html"
   :head   "topics/head.html"
   :header "topics/header.html"
   :content "topics/content.html"
   :footer "topics/footer.html"})

(def ^:private get-user-tmpls
  "Returns a map of user-defined templates if present, based on settings."
  (memoize
   (fn [] (into {}
                (map (fn [[k f]] (let [tmpl (f/file (*settings* :config-dir)
                                                    (*settings* :tmpl-dir) f)]
                                   (when (f/file? tmpl) {k tmpl})))
                     tmpl-location)))))

(defn- remove-all-hob-exts
  "Takes a file as input and returns its name, truncated of all hob extensions."
  [f]
  (second (re-matches #"(.+)\.hob(?:bes)?(?:\.txt)?" (f/base-name f))))

(defn- get-file-infos
  "Takes a file and returns a map containing various information about it."
  [file tree]
  {:title  (filename->title (remove-all-hob-exts file))
   :topic  (f/base-name (f/parent file))
   :date   (extract-date-components (f/mod-time file))
   :lang   (*settings* :lang)
   :author (*settings* :user)
   :words  (str (count-words (extract-strings tree)))
   :notice (not (*settings* :no-notice))
   :time   (compute-estimated-reading-time (extract-strings tree)
                                           (*settings* :wpm))
   :assoc  (engine/to-links (map remove-all-hob-exts
                                 (remove #(= file %)
                                         (get-hob-files-in-dir (f/parent file)))))})

(defn- transform
  "Takes a file as input and compiles it, returning a string."
  [file {:keys [abbrs vars]}]
  (let [tree (engine/parse (pre/preprocess (slurp file) abbrs vars))
        opts (get-file-infos file tree)]
    (gen/generate-course tree opts (get-user-tmpls))))

(defn- compile-hob
  "Takes and output dir, a file, and a map of options, and compiles the file,
  writing it to the output dir."
  [out file opts]
  (let [basename (remove-all-hob-exts file)
        output-f (f/file out "topics" (f/name (f/parent file)) (str basename
                                                                    ".html"))
        output (transform file opts)]
    (spit output-f output)))

(defn- get-param-file
  "Takes a root path, a key for the config directory and the function needed
  to parse it, and returns the result."
  [path config f]
  (f (f/file path (*settings* config ""))))

(defn transform-all
  "Takes an input dir and an output dir, and converts all hob files in the
  input dir to the output dir, effectively generating the final website."
  [in out]
  (let [mods  (atom (get-mod-times out))
        abbrs (get-param-file (*settings* :config-dir) :abbreviations parse-props)
        vars  (get-param-file (*settings* :config-dir) :variables eval-map)]
    (doseq [topic (get-topics-dirs in)]
      (copy-folder-with-assets in out topic)
      (let [hobs (get-hob-files-in-dir topic)
            l-ab (merge abbrs (get-param-file topic :abbreviations parse-props))
            l-va (merge vars (get-param-file topic :variables eval-map))]
        (dorun (pmap (fn [f] (when (> (f/mod-time f) (@mods (mod-key f) 0))
                               (compile-hob out f {:abbrs l-ab :vars l-va})
                               (swap! mods assoc (mod-key f) (f/mod-time f))))
                     hobs))))
    (write-mod-times out @mods)))

(defn- make-index-struct
  "Takes a list of topics and returns a list of the form [dir '(files)] for each
  output'd topic dir in root path. Used to send to engine/to-index-struct."
  [topics]
  (remove (fn [[_ f]] (empty? f))
          (map (fn [t] [(f/base-name t)
                        (map f/base-name (get-hob-files-in-dir t "*.html"))])
               topics)))

(defn make-index
  "Takes a root path and generates the index file of the compiled site."
  [path]
  (let [idx (make-index-struct (get-topics-dirs (f/file path "topics")))
        topics-n (str (count idx))
        courses-n (str (reduce + (map (fn [[_ files]] (count files)) idx)))
        idx-ast (engine/to-index-struct idx)
        out (gen/generate-index idx-ast
                                {:tn topics-n :cn courses-n
                                 :lang (*settings* :lang)
                                 :author (*settings* :user)
                                 :notice (not (*settings* :no-notice))
                                 :date (extract-date-components (java.util.Date.))}
                                (get-user-tmpls))]
    (spit (f/file path "index.html") out)))


; Public API
(defn compile-all
  "Takes an option map and compiles all given input dir into given output dir
  with given optional settings."
  [opts]
  (binding [*settings* opts]
    (let [user-assets (f/file (*settings* :config-dir) (*settings* :tmpl-dir) "assets")
          out (*settings* :output)]
      (if (f/directory? user-assets)
        (f/copy-dir-into user-assets (f/file out "assets"))
        (dump-resource (f/file out "assets")
                       "default/template/assets"))
      (transform-all (*settings* :input) out)
      (make-index out))))

(defn compile-one
  "Takes an option map and compiles the given file input into given output. If
  no output if given, prints to STDOUT."
  [opts]
  (let [file  (opts :input)
        abbrs (get-param-file (opts :config-dir) :abbreviations parse-props)
        vars  (get-param-file (opts :config-dir) :variables eval-map)
        tree  (engine/parse (pre/preprocess (slurp file) abbrs vars))
        gen   (gen/generate-standalone tree)]
    (if-let [out-f (opts :output)]
      (spit out-f gen)
      (println gen))))

(defn dump
  "Takes an option map containing at least an output dir and dumps the default
  configuration of Hobbes into it."
  [opts]
  (let [out (opts :output)]
    (dump-resource out "default")))

(defn delete-modtimes-file
  "Takes an option map containing at least an output dir and the name of the
  modtimes file, and deletes it.
  Used to allow for seemingly unnecessary re-compilation of courses."
  [opts]
  (binding [*settings* opts]
    (let [out (*settings* :output)]
      (f/delete (f/file out (*settings* :mod-times-file))))))
