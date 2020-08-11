(ns wcs.ui.example-article
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [clojure.edn :as edn]
    #?@(:clj
                    [[com.fulcrologic.fulcro.dom-server :as dom]
                     [clojure.pprint :refer [pprint]]]
        :cljs
                    [[com.fulcrologic.fulcro.dom :as dom]
                     [goog.object :as gobj]
                     ["vega" :as vg]
                     ["vega-embed" :as ve]
                     ["vega-lite" :as vl]
                     [cljs.pprint :refer [pprint]]
                     [cljs-http.client :as http]
                     [cljs.core.async :refer [<!]]
                     [cljs.core.async :refer-macros [go]]
                     #_[goog.labs.format.csv :as csv]]))
  #_(:require-macros
    #?@(:cljs
        [[cljs.core.async.macros :refer [go]]])))

#?(:cljs
   (defn parse-vl-spec [elem spec]
  (when spec
    (let [opts #js {"mode"     "vega-lite"
                    "renderer" "canvas"
                    "actions"  false
                    "tooltip" {"theme" "dark"}}
          js-spec (clj->js (assoc spec :$schema "https://vega.github.io/schema/vega-lite/v3.4.0.json"))]
      (ve elem js-spec opts)))))

#?(:cljs
   (defsc Chart [this _props]
  {:componentDidMount     (fn [this]
                            (when-let [dom-node (gobj/get this "div")]
                              (let [spec (comp/props this)]
                                (parse-vl-spec dom-node spec))))
   :shouldComponentUpdate (fn [this next-props _next-state]
                            (when-let [dom-node (gobj/get this "div")]
                              (let [new-spec next-props]
                                next-props
                                (parse-vl-spec dom-node new-spec)))
                            false)}
  (dom/div {:ref (fn [r] (gobj/set this "div" r))})))

(def chart
  #?(:cls
     nil
     :cljs
     (comp/factory Chart)))

(defn chart-spec [title data]
  (let [year-count (count (group-by :year data))]
    {:title title
     :width 400
     :height 400
     :data {:values data}
     :mark "line"
     :layer [
             {
              :encoding {
                         :x {:field "year" :type "temporal" :timeUnit "year" :axis {:tickCount year-count :title "year"}}
                         :y {:field "tonnage" :type "quantitative" :scale {:zero false} :axis {:title "tonnage"}}
                         :color {:field "council" :type "nominal"}}
              :layer [
                      {:mark {:type "line"
                              :point {:filled false
                                      :fill "white"}}}
                      {
                       :selection {
                                   :label {
                                           :type "single"
                                           :nearest true
                                           :on "mouseover"
                                           :encodings ["x"]
                                           :empty "none"
                                           }
                                   }
                       :mark "point"
                       :encoding {
                                  :opacity {
                                            :condition {:selection "label" :value 1}
                                            :value 0
                                            }
                                  }
                       }
                      ]
              }
             {
              :transform [{:filter {:selection "label"}}],
              :layer [
                      {
                       :mark {:type "rule" :color "gray"},
                       :encoding {
                                  :x {:type "temporal"  :field "year"}
                                  }
                       },
                      {
                       :encoding {
                                  :text {:type "nominal", :field "council"},
                                  :x {:type "temporal", :field "year"},
                                  :y {:type "quantitative", :field "tonnage"}
                                  },
                       :layer [
                               {
                                :mark {
                                       :type "text",
                                       :stroke "white",
                                       :strokeWidth 2,
                                       :align "left",
                                       :dx 5,
                                       :dy -5
                                       }
                                },
                               {
                                :mark {:type "text", :align "left", :dx 5, :dy -5},
                                :encoding {
                                           :color {:type "nominal", :field "council"}
                                           }
                                }
                               ]
                       }
                      ]
              }
             ]
     }))

(def dataset (atom nil))

#?(:cljs
(go
  (pprint "fetching waste-generated-per-council-citizen-per-year.edn ...")
  (let [response (<! (http/get "/datasets/waste-generated-per-council-citizen-per-year.edn"))]
    (pprint (str "response: " response))
    (reset! dataset (edn/read-string (:body response))))))

(defsc ExampleArticlePage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::ExampleArticlePage])
   :initial-state {}
   :route-segment ["articles" "whereabouts-is-recycling-improving"]}
  (dom/div
    (dom/h3 "Whereabouts is recycling improving?")

    (dom/p "The graph below indicates that the citizens of council X have the most improved recycling rate.")

    (chart (chart-spec "Waste generated per council citizen per year" @dataset))

    (dom/p
      (dom/a {:href "/datasets/waste-generated-per-council-citizen-per-year.csv"} "This")
      " is the " (dom/i "permanent") " hyperlink for the dataset on which this article is based.")))
