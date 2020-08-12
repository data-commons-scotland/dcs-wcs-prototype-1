(ns wcs.components.datasets
  (:require
    [clojure.string :as str]
    [ring.util.response :as resp]
    [taoensso.timbre :as log]
    [clojure.data.csv :as csv]
    [clojure.edn :as edn]
    [clj-http.client :as http]
    [clojure.set :refer [rename-keys]]
    [clojure.pprint :refer [pprint]])
  (:import
    java.net.URLEncoder
    java.time.LocalDate))


; Convert the CSV structure to a list-of-maps structure.
(defn to-maps [csv-data]
  (map zipmap (->> (first csv-data)
                   (map keyword)
                   repeat)
       (rest csv-data)))

; Ask statistic.gov.scot to execute the given SPARQL query
; and return its result as a list-of-maps.
(defn exec-query [sparql]
  (->> (http/post "http://statistics.gov.scot/sparql"
                  {:body (str "query=" (URLEncoder/encode sparql))
                   :headers {"Accept" "text/csv"
                             "Content-Type" "application/x-www-form-urlencoded"}
                   :debug false})
       :body
       csv/read-csv
       to-maps))

; Query for the waste tonnage generated per council citizen per year
(def sparql "

PREFIX qb: <http://purl.org/linked-data/cube#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX pdmx: <http://purl.org/linked-data/sdmx/2009/dimension#>
PREFIX sdmx: <http://statistics.gov.scot/def/dimension/>
PREFIX snum: <http://statistics.gov.scot/def/measure-properties/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?c ?y ?t2
WHERE {

    ?tonnage qb:dataSet <http://statistics.gov.scot/data/household-waste> .
    ?tonnage pdmx:refArea ?area .
    ?tonnage pdmx:refPeriod ?period .
    ?tonnage sdmx:wasteCategory ?wastecategory .
    ?tonnage sdmx:wasteManagement ?wastemanagement .
    ?tonnage snum:count ?t1 .

    ?wastecategory rdfs:label \"Total Waste\" .
    ?wastemanagement rdfs:label \"Waste Generated\" .

    ?population qb:dataSet <http://statistics.gov.scot/data/population-estimates-current-geographic-boundaries> .
    ?population pdmx:refArea ?area .
    ?population pdmx:refPeriod ?period .
    ?population sdmx:age <http://statistics.gov.scot/def/concept/age/all> .
    ?population sdmx:sex <http://statistics.gov.scot/def/concept/sex/all> .
    ?population snum:count ?p .

    ?area rdfs:label ?c .
    ?period rdfs:label ?y .
    BIND((xsd:integer(?t1)/xsd:integer(?p)) AS ?t2) .
}
")

(def dataset-sample
  (->> sparql
      exec-query
      (map #(rename-keys % {:c :council :y :year :t2 :tonnage}))
      (sort-by (juxt :c :y))))

(pprint dataset-sample)




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
                     rows (map #(vector (:council %) (:year %) (:tonnage %)) dataset-sample)
                     text (with-out-str
                            (csv/write-csv *out*
                                           (cons header-row
                                                 rows)))]
                 text)}

        "waste-generated-per-council-citizen-per-year.edn"
        {:status  200
         :headers {"Content-Type" "text/plain"
                   "Cache-Control"       "max-age=31536000, public, immutable"}
         :body (prn-str dataset-sample)}

        :default
        {:status  400
         :headers {"content-type" "text/plain"}
         :body    "Not found"}))

    (handler req))))

