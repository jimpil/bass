(defproject com.github.jimpil/bass "0.1.4-SNAPSHOT"
  :description "Clojure facilities for encoding/decoding to/from various numerical bases"
  :url "https://github.com/jimpil/bass"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.5"]]
  :repl-options {:init-ns com.github.jimpil.bass.core}
  :profiles {:dev {:dependencies [[commons-codec "1.22.0"]
                                  [mvxcvi/alphabase "3.0.185"]
                                  [criterium "0.4.6"]]}
             :test {:dependencies [[org.clojure/test.check "1.1.3"]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy" ]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ;["vcs" "push"]
                  ]
  :deploy-repositories [["releases" :clojars]] ;; lein release :patch
  :signing {:gpg-key "jimpil1985@gmail.com"}
  )
