(ns hobbes.core
  "Hobbes main namespace.
  Manages the whole program by stitching other namespaces together."
  (:require [hobbes.utils  :refer :all]
            [clojure.tools.cli :refer [parse-opts]]
            [me.raynes.fs :as f]
            [hobbes.runner :as run])
  (:gen-class))

(def ^:private version-infos
  "Map containing program version information."
  {:number "0.1.0"
   :date   "03/2015"
   :site   "hobbes-lang.org"
   :author {:name "Logan Braga"
            :mail "<braga.logan@gmail.com>"}})

(def ^:private static-settings
  "Map containing non-modifiable settings."
  {:tmpl-dir       "template"
   :abbreviations  "abbreviations.properties"
   :variables      "variables.edn"
   :mod-times-file ".modtimes.edn"})

(def ^:private cli-opts
  "Command-line options in tools.cli format."
  (let [not-blank? (complement clojure.string/blank?)
        conf-d (str (System/getProperty "user.home") java.io.File/separator
                    ".config" java.io.File/separator "hobbes")
        safe-int (fn [s] (try (Integer/parseInt s) (catch Exception e 200)))]
    [["-i" "--input INPUT" "Directory containing courses or file to compile"
      :validate-fn f/readable?]
     ["-o" "--output OUTPUT" "Directory for the generated site or output file."
      :validate-fn #(or (f/writeable? %) (f/writeable? (f/parent %)))]
     ["-c" "--config-dir DIR" "Directory containing config files" :default conf-d]
     ["-g" "--glob STRING" "Glob format string to select files to compile"
      :default "*.{hob,hobbes,hob.txt,hobbes.txt}" :id :glob-format
      :validate-fn not-blank?]
     ["-w" "--words-per-minute N"
      "Words per minute when computing estimated reading time"
      :default 200 :id :wpm :parse-fn safe-int :validate-fn pos?]
     ["-u" "--user USER" "Username to use as courses author in templates"
      :default (clojure.string/capitalize (System/getProperty "user.name"))]
     ["-l" "--lang LANG" "Language to use in templates metas"
      :default (System/getProperty "user.language")]
     ["-f" "--force" "Force compilation, even if courses are up-to-date"
      :default false]
     ["-v" "--verbose" "Verbose mode, printing status information at each step"]
     ["-h" "--help" "Show this help and exit" :default false]
     [nil "--no-notice"
      "Remove the Hobbes comment added at the start of every compiled file"
      :default false]
     [nil "--version" "Print version and exit" :default false]]))

(defn usage
  "Takes a summary string and returns command-line usage string."
  [summary]
  (clojure.string/join
   \newline
   ["Hobbes compiler - Compile hob lecture notes into static websites."
    ""
    "Usage: hobbes <action> [argument] [options]"
    ""
    "Options:"
    summary
    ""
    "Actions:"
    "  help            Shows this help and exits."
    "  compile INPUT   Takes a hob file or a directory and compiles it."
    "  dump OUTPUT     Takes a directory and dumps default settings inside it."
    "All action arguments can be overriden by the -i or -o options, depending on context."
    ""
    "Example commands:"
    "  `hobbes compile my_courses/ -w 350`"
    "  `hobbes compile -i my_courses/course.hob -o compiled.html`"
    "  `hobbes dump my_config/`"
    "  `hobbes dump --output=my_config/`"
    ""
    "Get more information at hobbes-lang.org."]))

(defn version
  "Prepares version information string."
  []
  (str "Hobbes v" (version-infos :number) " - " (version-infos :date)
       \newline
       " > " (version-infos :site)
       \newline \newline
       (get-in version-infos [:author :name])
       \newline
       (get-in version-infos [:author :mail])))

(defn parse-error
  "Takes an error sequence and returns a string containing them."
  [errors]
  (str "Errors occured during command parsing:\n"
       (clojure.string/join \newline (map #(str "  + " %) errors))
       "\n\n"
       "Use `hobbes help` for more information."))

(defn -main
  "Main entry point to Hobbes. Redirects program flow to wanted action by
  filtering and parsing command line arguments."
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-opts)
        settings (merge static-settings options)
        action   (first arguments)
        shortcut (second arguments)]
    (cond
      (:version options)        (exit 0 (version))
      (:help options)           (exit 0 (usage summary))
      errors                    (exit 1 (parse-error errors)))
    (case action
      "help"        (exit 0 (usage summary))
      "compile"     (let [opts (merge {:input shortcut} settings)
                          in   (opts :input)
                          out  (opts :output)]
                      (when (:force opts) (run/delete-modtimes-file opts))
                      (cond
                        (f/file? in)      (run/compile-one opts)
                        (f/directory? in) (run/compile-all opts)
                        :else             (exit 1 (usage summary))))
      "dump"        (let [opts (if (and shortcut
                                        (or (f/writeable? shortcut)
                                            (f/writeable? (f/parent shortcut))))
                                 (merge {:output shortcut} settings)
                                 settings)]
                      (if (nil? (opts :output))
                        (exit 1 (usage summary))
                        (run/dump opts)))
      (exit 1 (usage summary))))
  (shutdown-agents))
