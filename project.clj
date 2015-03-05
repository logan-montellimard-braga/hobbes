(defproject hobbes "0.1.0-SNAPSHOT"
  :description "Compiler transforming courses in .hob format to beautiful, local, static sites."
  :url "http://hobbes-lang.org"
  :license {:name "GNU/GPL v3"
            :url "http://www.gnu.org/licenses/gpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ; [seesaw "1.4.5" :exclusions [org.clojure/clojure]]
                 [me.raynes/fs "1.4.6"]
                 [enlive "1.1.5"]
                 [instaparse "1.3.5"]]
  :bin {:name "hobbes"}
  :codox {:src-dir-uri "http://github.com/loganbraga/hobbes/blob/master/"
          :src-linenum-anchor-prefix "L"}
  :main ^:skip-aot hobbes.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
