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
  [:.hob-date]  (e/content (.format (java.text.SimpleDateFormat. "dd-MM-yyyy")
                                    (java.util.Date.)))
  [:.hob-title] (e/content "")
  [:.hob-desc]  (e/content "")
  [:.hob-author (e/content (System/getProperty "user.name"))])

(e/defsnippet main (tmpl "content") [:main]
  [ast]
  identity)

(e/defsnippet footer (tmpl "footer") [:footer]
  [opts]
  [:.hob-prev-lecture] (e/content "")
  [:.hob-next-lecture] (e/content ""))

(e/deftemplate layout (clojure.java.io/resource "default/template/layout.html")
  [& args]
  [:html]   (e/set-attr :lang (System/getProperty "user.language"))
  [:head]   (e/substitute (head ""))
  [:header] (e/substitute (header ""))
  [:main]   (e/substitute (main ""))
  [:footer] (e/substitute (footer "")))

(defn generate
  [ast options & tmpl-dir])
