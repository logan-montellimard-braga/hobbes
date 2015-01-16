(defproject hobbes "0.1.0-SNAPSHOT"
  :description "Compiler transforming courses in .hob format to beautiful, local, static sites."
  :url "http://hobbes-lang.herokuapp.com"
  :license {:name "GNU/GPL v3"
            :url "http://www.gnu.org/licenses/gpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ; [org.clojure/tools.cli "0.3.1"]
                 ; [seesaw "1.4.5" :exclusions [org.clojure/clojure]]
                 [enlive "1.1.5"]
                 [instaparse "1.3.5"]]
  :bin {:name "hobbes"}
  :main ^:skip-aot hobbes.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
