(defproject dev.wstein.flixlab/allconstructs-clojure "0.1.0"
  :description "Comparative-reference Clojure port of src/AllConstructs.flix"
  :url "https://github.com/wstein/flix-specialization-names-lab"
  :license {:name "Apache-2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.12.0"]]
  :plugins [[lein-cljfmt "0.9.2"]]
  :main ^:skip-aot dev.wstein.flixlab.allconstructs.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
