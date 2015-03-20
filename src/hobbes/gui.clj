(ns hobbes.gui
  "Hobbes graphical user interface.
  Manages window rendering and calls dispatching.
  This namespace becomes the main namespace when compiled as :gui profile."
  (:require [hobbes.utils  :refer :all]
            [me.raynes.fs  :as f]
            [hobbes.runner :as run])
  (:use [seesaw core mig font chooser])
  (:gen-class))

; Set native mode as soon as the program starts.
; May be necessary on some platforms when the call inside -main isn't quick
; enough to enable it for the running process.
(native!)

(def ^:private hob-green
  "Hobbes logo green."
  "#92bc61")

(def ^:private hob-dark
  "Hobbes dark color."
  "#333333")

(def ^:private hob-font
  "Hobbes base font style."
  (font :name :sans-serif :size 11 :style #{:bold}))

(def ^:private state
  "Mutable state to send to hobbes.runner. Default settings."
  (atom {:input       nil
         :output      nil
         :config-dir  (f/file (f/home) ".config" "hobbes")
         :force       false
         :verbose     false
         :notice      true
         :user        (System/getProperty "user.name")
         :lang        (System/getProperty "user.language")
         :abbreviations  "abbreviations.properties"
         :variables      "variables.edn"
         :tmpl-dir    "template"
         :mod-times-file ".modtimes.edn"
         :glob-format "*.{hob,hobbes,hob.txt,hobbes.txt}"
         :wpm         200}))

(defn file-chooser [e]
  "Button action - opens a file chooser window."
  (let [frame (to-frame e)
        el    (id-of e)
        el-sel (keyword (str "#" (name el)))]
    (choose-file frame
                 :selection-mode :files-and-dirs
                 :type :open
                 :success-fn (fn [_ file]
                               (swap! state assoc el (f/normalized file))
                               (config! (select frame [el-sel]) :text
                                        (str "..." (f/base-name file)))))))

(defn do-action [e]
  "Switch hobbes.runner action based on event."
  (let [action (id-of e)]
    (case action
      :compile (cond
                 (nil? (@state :input)) (alert "L'entrée ne doit pas être nulle")
                 (nil? (@state :output)) (alert "La sortie ne doit pas être nulle")
                 :else (do
                         (when (@state :force) (run/delete-modtimes-file @state))
                         (cond
                           (f/file? (@state :input)) (run/compile-one @state)
                           (f/directory? (@state :input)) (run/compile-all @state)
                           :else nil)
                         (alert "Compilation effectuée avec succès.")))
      :dump    (if (nil? (@state :output))
                 (alert "La sortie ne doit pas être nulle.")
                 (do (run/dump @state)
                     (alert "Réglages par défaut copiés.")))
      nil)))

(def ^:private header-panel
  "First panel in the UI, with the Hobbes logo."
  (label :icon (clojure.java.io/resource "gui/header.png")
         :id :header
         :halign :center :valign :center :border 0))

(def ^:private buttons-panel
  "Second panel in the UI, with buttons for the main actions: compile or dump
  resources."
  (grid-panel :columns 2
              :id :buttons-panel
              :border 0 :hgap 8 :vgap 10
              :background :white :foreground hob-green
              :items [(button :class :action :id :compile :text "COMPILER")
                      (button :class :action :id :dump :text "DUMPER")]))

(def ^:private files-panel
  "Third panel in the UI, with input/output/config dirs or files selectors."
  (grid-panel :columns 2
              :border 15 :hgap 8 :vgap 10
              :id :files-panel
              :background hob-green :foreground :white
              :items [(label :tip "Dossier ou fichier d'origine à compiler"
                             :text "Entrée *")
                      (button :id :input :class :select-file :text "CHOISIR...")
                      (label :tip "Dossier ou fichier de destination pour le résultat"
                             :text "Sortie *")
                      (button :id :output :class :select-file :text "CHOISIR...")
                      (label :tip "Dossier de configuration Hobbes"
                             :text "Configuration")
                      (button :id :config-dir :class :select-file :text "CHOISIR...")]))

(def ^:private options-panel
  "Last panel in the UI, with user configuration options."
  (grid-panel :columns 3
              :border 15 :hgap 8 :vgap 4
              :id :options-panel
              :background hob-green :foreground :white
              :items [(label :class "title" :text "OPTIONS") "" ""
                      (checkbox :text "Force"   :selected? false
                                :tip "Forcer la recompilation de tous les cours"
                                :id :force :class :listen)
                      (checkbox :text "Notice"  :selected? true
                                :tip "Ajouter la notice Hobbes à chaque fichier"
                                :id :notice :class :listen)
                      ""
                      "" "" ""
                      (label :text "Auteur")
                      (label :text "Langue")
                      (label :text "Mots/min")
                      (text :text (@state :user)
                            :tip "Nom à utiliser en tant qu'auteur des cours"
                            :id :user :class :listen)
                      (text :text (@state :lang)
                            :tip "Langue à utiliser dans la génération des cours"
                            :id :lang :class :listen)
                      (spinner :class :listen :id :wpm
                               :tip "Mots par minute lors du calcul du temps de lecture"
                               :model (spinner-model 200 :from 10 :to 2000 :by 10))]))

(def main-layout
  "Hobbes GUI main layout, stitching all panels together."
  (mig-panel :border 0
             :constraints ["fill" "grow"]
             :background :white
             :items [[header-panel  "grow, wrap"]
                     [buttons-panel "grow, wrap"]
                     [files-panel   "grow, wrap"]
                     [options-panel "grow, wrap"]]))

(defn style-frame
  "Style all UI elements from a given frame f."
  [f]
  (let [labels (select f [:JLabel])
        btns-g (select files-panel [:JButton])
        btns-w (select buttons-panel [:JButton])
        txts   (select f [:JTextField])
        spin   (select f [:JSpinner])
        chkbox (select f [:JCheckBox])]
    (config! labels :foreground :white :background hob-green :font hob-font)
    (config! btns-g :background :white :foreground hob-green :font hob-font
             :focusable? false)
    (config! btns-w :background hob-green :foreground :white :font hob-font
             :focusable? false)
    (config! txts   :foreground hob-dark :background :white)
    (config! spin :background :white :foreground hob-green)
    (config! chkbox :foreground :white :background hob-green :font hob-font
             :focusable? false))
  f)

(defn add-listeners
  "Attach listeners to UI elements from a given frame f."
  [f]
  (doseq [el (select f [:.listen])]
    (listen el :selection #(swap! state assoc (id-of el) (value %))))
  (doseq [btn (select f [:.select-file])]
    (listen btn :action file-chooser))
  (doseq [btn (select f [:.action])]
    (listen btn :mouse-clicked do-action))
  f)

(defn display
  "Takes a frame and renders it on screen."
  [f]
  (-> f pack! show!))

(def hobbes-frame
  "Function invoked at startup. Renders Hobbes window."
  (frame :title "Hobbes"
         :on-close :dispose
         :icon (clojure.java.io/resource "gui/icon.png")
         :resizable? false
         :content main-layout))

(defn- show-frame
  "Init whole Hobbes GUI window."
  []
  (native!)
  (invoke-later (-> hobbes-frame
                    style-frame
                    add-listeners
                    display)))

(defn -main
  "GUI entry point."
  [& args]
  (show-frame))
