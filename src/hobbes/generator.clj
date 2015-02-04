(ns hobbes.generator
  "Generator namespace, used to transform valid HTML5 AST into complete web
  pages."
  (:require [net.cgrand.enlive-html :as e]
            [hobbes.utils :refer :all]))

(def ^:private base-path
  "Contains the base, default path for the templates directory"
  (atom "default/template/"))

(defn- set-base-path
  "Sets the base path of the corresponding atom to provided input."
  [path]
  (reset! base-path path))

(defn- get-tmpl-name
  "Takes a string as input and returns the path to the corresponding html
  template as a string."
  [string]
  (clojure.java.io/resource (str @base-path string ".html")))

(def ^:private tmpl-list
  "Mutable map atom containing dynamically defined enlive templates."
  (atom {}))

(def ^:private snippet-list
  "Mutable map atom containing dynamically defined enlive snippets."
  (atom {}))

(defmacro ^:private deftemplate-dynamic
  "Takes a symbol name, a source path string, args and defining forms and
  generates an enlive template dynamically. Used to allow for different path
  for same template based on arguments."
  [name source args & forms]
  `(defn- ~name [& args#]
     (let [src# (get-tmpl-name ~source)]
       (if-let [template# (get @tmpl-list src#)]
         (apply template# args#)
         (let [template# (e/template src# ~args ~@forms)]
           (swap! tmpl-list assoc src# template#)
           (apply template# args#))))))

(defmacro ^:private defsnippet-dynamic
  "Takes a symbol name, a source path string, a root selector, args and
  defining forms and generates an enlive snippet dynamically.
  Used to allow for different path for same snippet based on arguments."
  [name source selector args & forms]
  `(defn- ~name [& args#]
     (let [src# (get-tmpl-name ~source)]
       (if-let [snippet# (get @snippet-list src#)]
         (apply snippet# args#)
         (let [snippet# (e/snippet src# ~selector ~args ~@forms)]
           (swap! snippet-list assoc src# snippet#)
           (apply snippet# args#))))))

(defsnippet-dynamic head "topics/head" [:head]
  [m]
  [[:meta (e/attr= :name "author")]]      (e/set-attr :content (m :author))
  [[:meta (e/attr= :name "description")]] (e/set-attr :content (m :desc))
  [:title]                                (e/content  (m :title)))

(defsnippet-dynamic header "topics/header" [:header]
  [m]
  [:.hob-date]   (e/content (date-now))
  [:.hob-title]  (e/content (m :title))
  [:.hob-desc]   (e/content (m :desc))
  [:.hob-author] (e/content (m :author)))

(defsnippet-dynamic main "topics/content" [:main]
  [tree]
  [:.hob-content] (e/content tree))

(defsnippet-dynamic footer "topics/footer" [:footer]
  [m]
  [:.hob-prev-lecture] (e/content (m :prev))
  [:.hob-next-lecture] (e/content (m :next)))

(deftemplate-dynamic layout "layout"
  [tree opts klass]
  [:html]   (e/set-attr   :lang (System/getProperty "user.language"))
  [:body]   (e/add-class  (name klass))
  [:head]   (e/substitute (head   opts))
  [:header] (e/substitute (header opts))
  [:main]   (e/substitute (main   tree))
  [:footer] (e/substitute (footer opts)))


; Public API
(defn generate-course
  "Takes an enlive-like AST as input and a map of options, and returns the html
  generated with the course layout as a string."
  [tree options & tmpl-dir]
  (apply str (layout tree options :course)))

(defn generate-topic
  "Takes an enlive-like AST as input and a map of options, and returns the html
  generated with the topic-page layout as a string."
  [tree options & tmpl-dir])

(defn generate-index
  "Takes an enlive-like AST as input and a map of options, and returns the html
  generated with the index-page layout as a string."
  [tree options & tmpl-dir])
