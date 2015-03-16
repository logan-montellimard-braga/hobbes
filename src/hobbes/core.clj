(ns hobbes.core
  "Hobbes main namespace.
  Manages the whole program by stitching other namespaces together."
  (:require [hobbes.utils        :refer :all]
            [hobbes.preprocessor :as pre]
            [hobbes.generator    :as gen]
            [hobbes.engine       :as engine]
            [me.raynes.fs        :as f])
  (:gen-class))

(def ^:private default-settings
  "Map containing default hobbes settings."
  {:input-dir      "topics"
   :output-dir     "_site"
   :config-dir     "_config"
   :tmpl-dir       "template"
   :abbreviations  "abbreviations.properties"
   :variables      "variables.edn"
   :glob-format    "*.{hob,hobbes,hob.txt,hobbes.txt}"
   :mod-times-file ".modtimes.edn"
   :wpm            200})

(def ^:private special-files-glob
  "String of glob pattern for all hobbes special files."
  (str "{" (clojure.string/join "," [(default-settings :abbreviations)
                                     (default-settings :variables)
                                     (default-settings :glob-format)]) "}"))

(defn- get-mod-times
  "Get the modification times of already compiled hob files as a map from
  default location."
  [path]
  (eval-map (f/file path (default-settings :output-dir)
                    (default-settings :mod-times-file))))

(defn- write-mod-times
  "Sets the modification times of already compiled hob files as an edn map to
  default location."
  [path edn]
  (spit (f/file path (default-settings :output-dir)
                (default-settings :mod-times-file)) (pr-str edn)))

(defn- mod-key
  "Generates the storage key inside the modtimes edn file for a given file."
  [file]
  (str (f/name (f/parent file)) "/" (f/name file)))

(defn- get-topics-dirs
  "Takes a string root path and returns a seq of all directories supposed to
  represent course topics, i.e. directories inside the path/<input-dir>/ dir.
  Returns nil if path is not a directory or if no files were found."
  [path]
  (when (f/directory? path)
    (let [input (f/file path (default-settings :input-dir))]
      (filter f/directory? (f/list-dir input)))))

(defn- get-hob-files-in-dir
  "Takes a directory path and returns a seq of all files to treat inside it.
  Returns nil if path is not a directory or if no files were found."
  [path & [glob-format]]
  (let [glob (or glob-format (default-settings :glob-format))]
    (filter f/file? (f/glob path glob))))

(defn- copy-folder-with-assets
  "Takes a string root path and a string folder name and creates the directory
  inside the predefined output-dir in the root path, copying all assets inside
  and removing hob files."
  [path topic]
  (let [from (f/file path (default-settings :input-dir) (f/name topic))
        to (f/file path (default-settings :output-dir) "topics" (f/name topic))]
    (f/copy-dir-into from to)
    (dorun (map f/delete
                (get-hob-files-in-dir to special-files-glob)))))

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

(defn- get-user-tmpls
  "Takes a root path and returns a map of user-defined templates if present."
  [path]
  (into {}
        (map (fn [[k f]] (let [tmpl (f/file path (default-settings :config-dir)
                                            (default-settings :tmpl-dir) f)]
                           (when (f/file? tmpl) {k tmpl})))
             tmpl-location)))

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
   :lang   (System/getProperty "user.language")
   :author (clojure.string/capitalize (System/getProperty "user.name"))
   :words  (str (count-words (extract-strings tree)))
   :time   (compute-estimated-reading-time (extract-strings tree)
                                           (default-settings :wpm))
   :assoc  (engine/to-links (map remove-all-hob-exts
                                 (remove #(= file %)
                                         (get-hob-files-in-dir (f/parent file)))))})

(defn- transform
  "Takes a file as input and compiles it, returning a string."
  [path file {:keys [abbrs vars]}]
  (let [tree (engine/parse (pre/preprocess (slurp file) abbrs vars))
        opts (get-file-infos file tree)]
    (gen/generate-course
     tree opts (get-user-tmpls (f/file path)))))

(defn- compile-hob
  "Takes a root path and a file, and compiles the file, writing it to
  <output-dir>/topics/<topic>/<file>.html."
  [path file opts]
  (let [basename (remove-all-hob-exts file)
        output-f (f/file path (default-settings :output-dir) "topics"
                         (f/name (f/parent file)) (str basename ".html"))
        output (transform path file opts)]
    (spit output-f output)))

(defn- get-param-file
  "Takes a root path, a key for the config directory, the key to find inside it
  and the function needed to parse it, and returns the result."
  [path config k f]
  (f (f/file path (default-settings config "") (default-settings k))))

(defn transform-all
  "Takes a root path as input and converts all hob files in the
  <input-dir>/<topic> directories, effectively generating the final website."
  [path]
  (let [mods  (atom (get-mod-times path))
        abbrs (get-param-file path :config-dir :abbreviations parse-props)
        vars  (get-param-file path :config-dir :variables eval-map)]
    (doseq [topic (get-topics-dirs path)]
      (copy-folder-with-assets path topic)
      (let [hobs (get-hob-files-in-dir topic)
            l-ab (merge abbrs (get-param-file topic nil :abbreviations parse-props))
            l-va (merge vars (get-param-file topic nil :variables eval-map))]
        (dorun (pmap (fn [f] (when (> (f/mod-time f) (@mods (mod-key f) 0))
                               (compile-hob path f {:abbrs l-ab :vars l-va})
                               (swap! mods assoc (mod-key f) (f/mod-time f))))
                     hobs))))
    (write-mod-times path @mods)))

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
  (let [idx (make-index-struct (get-topics-dirs
                                (f/file path (default-settings :output-dir))))
        topics-n (str (count idx))
        courses-n (str (reduce + (map (fn [[_ files]] (count files)) idx)))
        idx-ast (engine/to-index-struct idx)
        out (gen/generate-index idx-ast
                                {:tn topics-n :cn courses-n
                                 :lang (System/getProperty "user.language")
                                 :author (clojure.string/capitalize
                                          (System/getProperty "user.name"))
                                 :date (extract-date-components (java.util.Date.))}
                                (get-user-tmpls (f/file path)))]
    (spit (f/file path (default-settings :output-dir) "index.html") out)))

(defn -main
  "Main entry point to Hobbes"
  [folder & args]
  (when-not (f/directory? folder)
    (throw (java.io.FileNotFoundException. (str folder " does not exists."))))
  (let [user-assets (f/file folder (default-settings :config-dir)
                            (default-settings :tmpl-dir) "assets")
        output (f/file folder (default-settings :output-dir) "assets")]
    (print "Starting compilation...")
    (if (f/directory? user-assets)
      (f/copy-dir-into user-assets output)
      (dump-resource output "default/template/assets"))
    (transform-all folder)
    (make-index folder)
    (println "    DONE!")
    (shutdown-agents)))
