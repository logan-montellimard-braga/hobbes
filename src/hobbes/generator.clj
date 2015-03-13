(ns hobbes.generator
  "Generator namespace, used to transform valid HTML5 AST into complete web
  pages."
  (:require [net.cgrand.enlive-html :as e]
            [hobbes.utils           :refer :all]))

(def ^:private default-templates
  "Map containing the default templates as files."
  (let [default-path "default/template/"
        tmpl (fn [s] (clojure.java.io/resource (str default-path s ".html")))]
    (atom
     {:layout  (tmpl "layout")
      :index   (tmpl "index")
      :head    (tmpl "topics/head")
      :header  (tmpl "topics/header")
      :content (tmpl "topics/content")
      :footer  (tmpl "topics/footer")})))

(defn- set-templates
  "Takes a map m and merges its content with the default-templates map,
  effectively overriding default template files with provided ones."
  [m]
  (swap! default-templates merge m))

(def ^:private tmpl-list
  "Mutable map atom containing dynamically defined enlive templates."
  (atom {}))

(def ^:private snippet-list
  "Mutable map atom containing dynamically defined enlive snippets."
  (atom {}))

(defmacro ^:private deftemplate-lazy
  "Takes a symbol name, a source path string, args and defining forms and
  generates an enlive template dynamically. Used to allow for different path
  for same template based on arguments."
  [label source args & forms]
  `(defn- ~label [& args#]
     (let [src# (@default-templates ~source)]
       (if-let [template# (get @tmpl-list src#)]
         (apply template# args#)
         (let [template# (e/template src# ~args ~@forms)]
           (swap! tmpl-list assoc src# template#)
           (apply template# args#))))))

(defmacro ^:private defsnippet-lazy
  "Takes a symbol name, a source path string, a root selector, args and
  defining forms and generates an enlive snippet dynamically.
  Used to allow for different path for same snippet based on arguments."
  [label source selector args & forms]
  `(defn- ~label [& args#]
     (let [src# (@default-templates ~source)]
       (if-let [snippet# (get @snippet-list src#)]
         (apply snippet# args#)
         (let [snippet# (e/snippet src# ~selector ~args ~@forms)]
           (swap! snippet-list assoc src# snippet#)
           (apply snippet# args#))))))

(defsnippet-lazy head :head [:head]
  [m]
  [[:meta (e/attr= :name "author")]]      (e/set-attr :content (m :author))
  [:title]                                (e/content  (m :title)))

(defsnippet-lazy header :header [:header]
  [m]
  [:.hob-date :.d]       (e/content (get-in m [:date :d]))
  [:.hob-date :.m]       (e/content (get-in m [:date :m]))
  [:.hob-date :.y]       (e/content (get-in m [:date :y]))
  [:.hob-date :.t-h]     (e/content (get-in m [:date :t-h]))
  [:.hob-date :.t-m]     (e/content (get-in m [:date :t-m]))
  [:.hob-date :.t-s]     (e/content (get-in m [:date :t-s]))
  [:.hob-date :.mf]      (e/content (get-in m [:date :mf]))
  [:.hob-date :.df]      (e/content (get-in m [:date :df]))
  [:.hob-time :.minutes] (e/content (get-in m [:time :m]))
  [:.hob-time :.seconds] (e/content (get-in m [:time :s]))
  [:.hob-title]          (e/content (m :title))
  [:.hob-author]         (e/content (m :author)))

(defsnippet-lazy main :content [:main]
  [m tree]
  [:.hob-content] (e/content tree)
  [:.hob-title]   (e/content (m :title))
  [:.hob-topic]   (e/content (m :topic)))

(defsnippet-lazy footer :footer [:footer]
  [m]
  [:.hob-associated] (e/content (m :assoc)))

(deftemplate-lazy layout :layout
  [tree opts klass]
  [:html]   (e/set-attr   :lang (opts :lang))
  [:body]   (e/add-class  (name klass))
  [:head]   (e/substitute (head   opts))
  [:header] (e/substitute (header opts))
  [:main]   (e/substitute (main   opts tree))
  [:footer] (e/substitute (footer opts)))

(deftemplate-lazy index :index
  [tree opts klass]
  [:html]                 (e/set-attr :lang (opts :lang))
  [:body]                 (e/add-class (name klass))
  [:.hob-date :.d]        (e/content (get-in opts [:date :d]))
  [:.hob-date :.m]        (e/content (get-in opts [:date :m]))
  [:.hob-date :.y]        (e/content (get-in opts [:date :y]))
  [:.hob-date :.t-h]      (e/content (get-in opts [:date :t-h]))
  [:.hob-date :.t-m]      (e/content (get-in opts [:date :t-m]))
  [:.hob-date :.t-s]      (e/content (get-in opts [:date :t-s]))
  [:.hob-date :.mf]       (e/content (get-in opts [:date :mf]))
  [:.hob-date :.df]       (e/content (get-in opts [:date :df]))
  [:.hob-content]         (e/content tree)
  [:.hob-author]          (e/content (opts :author))
  [[:meta (e/attr= :name "author")]] (e/set-attr :content (opts :author))
  [:.hob-stats :.topics]  (e/substitute (opts :tn))
  [:.hob-stats :.courses] (e/substitute (opts :cn)))


; Public API
(defn generate-course
  "Takes an enlive-like AST as input and a map of options, and returns the html
  generated with the course layout as a string."
  [tree options tmpl-map]
  (do (set-templates tmpl-map)
      (clojure.string/join (layout tree options :course))))

(defn generate-index
  "Takes an enlive-like AST as input and a map of options, and returns the html
  generated with the index-page layout as a string."
  [tree options tmpl-map]
  (do (set-templates tmpl-map)
      (clojure.string/join (index tree options :index))))
