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
  {:input-dir  "topics"
   :output-dir "_site"
   :glob-format "*.{hob,hobbes,hob.txt,hobbes.txt}"})

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
  [path]
  (filter f/file? (f/glob path (default-settings :glob-format))))

(defn- copy-folder-with-assets
  "Takes a string root path and a string folder name and creates the directory
  inside the predefined outpu-dir in the root path, copying all assets inside
  and removing hob files."
  [path topic]
  (let [from (f/file path (default-settings :input-dir) (f/name topic))
        to (f/file path (default-settings :output-dir) "topics" (f/name topic))]
    (f/copy-dir-into from to)
    (dorun (map f/delete (get-hob-files-in-dir to)))))

(defn copy-template-assets
  "Prepares compilation by creating the file structure for output."
  [path]
  (let [output (f/file path (default-settings :output-dir))]
    (f/mkdir output)))

(defn- transform
  [file])

(defn- transform-all
  "Takes a root path as input and converts all hob files in the
  <input-dir>/<topic> directories, effectively generating the final website."
  [path]
  (doseq [topic (get-topics-dirs path)]
    (copy-folder-with-assets path topic)
    (dorun (pmap transform (get-hob-files-in-dir topic)))))

(defn -main
  "Main entry point to Hobbes"
  [& args])
