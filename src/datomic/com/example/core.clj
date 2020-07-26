(ns com.example.core
  (:require [development])
  (:gen-class))

(defn -main
      "For starting as a JAR file."
      []
      (println "Calling development/start ...")
      (development/start))