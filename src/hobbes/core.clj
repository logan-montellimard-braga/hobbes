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
  {:input-dir  "topics"
   :output-dir "_site"
   :glob-format "*.{hob,hobbes,hob.txt,hobbes.txt}"
   :mod-times-file ".modtimes.edn"})

(defn- get-mod-times
  "Get the modification times of already compiled hob files as a map from
  default location."
  [path]
  (try (eval-map (f/file path (default-settings :mod-times-file)))
       (catch Exception e {})))

(defn- write-mod-times
  "Sets the modification times of already compiled hob files as an edn map to
  default location."
  [path edn]
  (spit (f/file path (default-settings :mod-times-file)) (pr-str edn)))

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
    (dorun (map f/delete (get-hob-files-in-dir to)))))

(defn- copy-template-assets
  "Moves template assets to outpout dir."
  [path]
  (let [output (f/file path (default-settings :output-dir))]
    (f/mkdir output)))

(defn- transform
  "Takes a file as input and compiles it, returning a string."
  [file]
  (-> file
      (slurp)
      (pre/preprocess {} {})
      (engine/parse)
      (gen/generate-course {} {})))

(defn- compile-hob
  "Takes a root path and a file, and compiles the file, writing it to
  <output-dir>/topics/<topic>/<file>.html."
  [path file]
  (let [output (transform file)]
    (spit (f/file path (default-settings :output-dir) "topics"
                  (f/name (f/parent file))
                  (str (second (re-matches #"(.+)\.hob(?:bes)?(?:\.txt)?"
                                           (f/name file)))
                       ".html"))
          output)))

(defn- make-index-list
  "Takes a root path and returns a list of enlive-compatible maps of topics
  and courses in them. Used to send to gen/generate-index."
  [path]
  (let [topics (get-topics-dirs (f/file path (default-settings :output-dir)))
        src (fn [f] (str "topics/" (f/name (f/parent f)) "/" (f/name f) ".html"))
        li  (fn [f] {:tag :li :content {:tag :a :content (f/base-name f true)
                                        :attrs {:href (src f)}}})]
    (flatten
     (map (fn [t] (list {:tag :h3 :content (f/base-name t)}
                        {:tag :ul
                         :content (map li (get-hob-files-in-dir t "*.html"))}))
          topics))))

(defn- transform-all
  "Takes a root path as input and converts all hob files in the
  <input-dir>/<topic> directories, effectively generating the final website."
  [path]
  (let [mods (atom (get-mod-times path))]
    (doseq [topic (get-topics-dirs path)]
      (copy-folder-with-assets path topic)
      (let [hobs (get-hob-files-in-dir topic)]
        (dorun (pmap
                 (fn [f] (when (> (f/mod-time f)
                                  (@mods (str (f/name topic) "/" (f/name f)) 0))
                           (compile-hob path f)
                           (swap! mods assoc (str (f/name topic) "/" (f/name f))
                                  (f/mod-time f))))
                 hobs))))
    (write-mod-times path @mods)))

(defn -main
  "Main entry point to Hobbes"
  [& args])
