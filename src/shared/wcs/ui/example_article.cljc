(ns wcs.ui.example-article
  (:require
    #?@(:clj
                    [[com.fulcrologic.fulcro.dom-server :as dom]
                     [clojure.pprint :refer [pprint]]]
        :cljs
                    [[com.fulcrologic.fulcro.dom :as dom]
                     [goog.object :as gobj]
                     ["vega" :as vg]
                     ["vega-embed" :as ve]
                     ["vega-lite" :as vl]
                     [cljs.pprint :refer [pprint]]])
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]))

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

(def data
  [{:council "East Renfrewshire"   :year "2015"   :tonnage "0.5087279607717727"}
   {:council "East Renfrewshire"   :year "2016"   :tonnage "0.4087279607717727"}
   {:council "Scottish Borders "  :year "2015"  :tonnage "0.43802209750964577"}
   {:council "Scottish Borders "  :year "2016"  :tonnage "0.33802209750964577"}
   {:council "Clackmannanshire"   :year "2015"   :tonnage "0.5183232087227415"}
   {:council "Clackmannanshire"   :year "2016"   :tonnage "0.5083232087227415"}
   {:council "Orkney Islands"   :year "2015"  :tonnage "0.46059125732311856"}
   {:council "Orkney Islands"   :year "2016"  :tonnage "0.96059125732311856"}])

(defsc ExampleArticlePage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::ExampleArticlePage])
   :initial-state {}
   :route-segment ["waste-generated-per-council-citizen-per-year"]}
  (dom/div
    (dom/h3 "Waste generated per council citizen per year")

    (dom/p "TODO")
    (chart (chart-spec "Waste generated per council citizen per year" data))))
