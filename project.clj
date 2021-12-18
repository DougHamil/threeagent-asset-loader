(defproject doughamil/threeagent-asset-loader "0.0.1-SNAPSHOT"
  :description "Easier asset loading for threeagent apps"
  :url "https://github.com/DougHamil/threeagent-asset-loader"
  :license {:name "MIT"}

  :deploy-repositories [["releases" {:url "https://clojars.org/repo/"
                                     :signing {:gpg-key "C89350FC"}
                                     :username :env
                                     :password :env}]
                        ["snapshots" {:url "https://clojars.org/repo/"
                                      :signing {:gpg-key "C89350FC"}
                                      :username :env
                                      :password :env}]]

  :dependencies [camel-snake-kebab "0.4.2"]

  :source-paths ["src"]

  :plugins [[lein-doo "0.1.10"]
            [lein-shell "0.5.0"]]

  :profiles {:test {:dependencies [[thheller/shadow-cljs "2.10.15"]]
                    :source-paths ["test"]}}

  :doo {:paths {:karma "node_modules/.bin/karma"}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["shell" "git" "commit" "-am" "Version ${:version} [ci skip]"]
                  ["vcs" "tag" "v" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["shell" "git" "commit" "-am" "Version ${:version} [ci skip]"]
                  ["shell" "git" "checkout" "main"]
                  ["shell" "git" "merge" "release"]
                  ["vcs" "push"]])                       
  
