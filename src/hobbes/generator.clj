(ns hobbes.generator
  "Generator namespace, used to transform valid HTML5 AST into complete web
  pages."
  (:require [net.cgrand.enlive-html :as e]
            [hobbes.utils :refer :all]))

(defn- tmpl
  "Takes a string as input and returns the path to the corresponding enlive
  template as a string."
  [string]
  (clojure.java.io/resource (str "default/template/topics/" string ".html")))

(e/defsnippet head (tmpl "head") [:head]
  [opts]
  [(e/attr= :name "author")] (e/set-attr
                               :content (System/getProperty "user.name"))
  [:title]                   (e/content "foo"))

(e/defsnippet header (tmpl "header") [:header]
  [opts]
  [:.hob-date]   (e/content (.format (java.text.SimpleDateFormat. "dd-MM-yyyy")
                                     (java.util.Date.)))
  [:.hob-title]  (e/content "")
  [:.hob-desc]   (e/content "")
  [:.hob-author] (e/content (System/getProperty "user.name")))

(e/defsnippet main (tmpl "content") [:main]
  [tree]
  [:.hob-content] (e/content tree))

(e/defsnippet footer (tmpl "footer") [:footer]
  [opts]
  [:.hob-prev-lecture] (e/content "")
  [:.hob-next-lecture] (e/content ""))

(e/deftemplate layout (clojure.java.io/resource "default/template/layout.html")
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
