(ns wcs.core
  (:require
    #_[clojure.tools.cli :as cli]
    [mount.core :as mount]
    [development :as dev])
  (:gen-class))

#_(defn parse-opt-vals [args]
        (let [opts [["-h" "--host HOST" "Host address"]
                    ["-p" "--port PORT" "Port number"]]]
             (:options (cli/parse-opts args opts))))

(defn -main
      "For starting as a JAR file."
      [& args]
      (println "Backend is starting")
      (mount/start-with-args {:config "config/prod.edn"
                              :overrides {:org.immutant.web/config {:port (Integer/parseInt (System/getenv "PORT"))  #_8085
                                                                    :host "0.0.0.0"}}}) #_"192.168.1.80"
      (dev/seed!))
