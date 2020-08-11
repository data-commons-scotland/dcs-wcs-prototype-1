(ns wcs.components.datasets
  (:require
    [clojure.string :as str]
    [ring.util.response :as resp]
    [taoensso.timbre :as log]
    [clojure.data.csv :as csv]
    [clojure.edn :as edn]))



(def dataset-example
  [{:council "East Renfrewshire"   :year "2015"   :tonnage "0.5087279607717727"}
   {:council "East Renfrewshire"   :year "2016"   :tonnage "0.4087279607717727"}
   {:council "Scottish Borders "  :year "2015"  :tonnage "0.43802209750964577"}
   {:council "Scottish Borders "  :year "2016"  :tonnage "0.33802209750964577"}
   {:council "Clackmannanshire"   :year "2015"   :tonnage "0.5183232087227415"}
   {:council "Clackmannanshire"   :year "2016"   :tonnage "0.5083232087227415"}
   {:council "Orkney Islands"   :year "2015"  :tonnage "0.46059125732311856"}
   {:council "Orkney Islands"   :year "2016"  :tonnage "0.66059125732311856"}])






(defn wrap-datasets
"Middleware that can serve a dataset file at URI `base-path`/`slug`."
[handler base-path]
(fn [{:keys [uri params] :as req}]
  (if (str/starts-with? uri base-path)
    (let [slug (last (str/split uri #"/"))
          x (:x params)]
      (log/info "Trying to serve dataset " slug)
      (condp = slug

        "waste-generated-per-council-citizen-per-year.csv"
        {:status  200
         :headers {"Content-Disposition" "attachment;filename=waste-per-council-citizen-per-year.csv"
                   "Content-Type" "text/plain"
                   "Cache-Control"       "max-age=31536000, public, immutable"}
         :body (let [header-row ["council" "year" "tonnage"]
                     rows (map #(vector (:council %) (:year %) (:tonnage %)) dataset-example)
                     text (with-out-str
                            (csv/write-csv *out*
                                           (cons header-row
                                                 rows)))]
                 text)}

        "waste-generated-per-council-citizen-per-year.edn"
        {:status  200
         :headers {"Content-Type" "text/plain"
                   "Cache-Control"       "max-age=31536000, public, immutable"}
         :body (prn-str dataset-example)}

        :default
        {:status  400
         :headers {"content-type" "text/plain"}
         :body    "Not found"}))

    (handler req))))

