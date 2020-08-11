(ns wcs.ui.autocomplete
  (:require
    #?@(:clj
        [[com.fulcrologic.fulcro.dom-server :as dom]
         [clojure.pprint :refer [pprint]]]
        :cljs
        [[com.fulcrologic.fulcro.dom :as dom]
         [goog.functions :as gf]
         [com.fulcrologic.fulcro.data-fetch :as df]
         #_[com.fulcrologic.semantic-ui.modules.rating.ui-rating :refer [ui-rating]]
         [cljs.pprint :refer [pprint]]])
    [clojure.string :as str]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.rad.routing :as rroute]
    [wcs.ui.example-article :as example-article]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def all-indexed-data
  [{:title "Aberdeenshire Council - recycling centres"
    :additional "waste dump tip"
    :url "https://www.aberdeenshire.gov.uk/online/open-data/"
    :type "portal"
    :rating "**"}
   {:title "Dundee City Council - bin sensor returns"
    :additional "waste"
    :url "https://data.dundeecity.gov.uk/dataset/bin-sensor-returns"
    :type "dataset"
    :rating "***"}
   {:title "Dundee City Council - recycling points"
    :additional "waste dump tip"
    :url "https://data.dundeecity.gov.uk/dataset/recycling-facility-locations"
    :type "dataset"
    :rating "***"}
   {:title "East Renfrewshire Council - bin collection days"
   :additional "waste domestic household"
   :url "https://ckan.publishing.service.gov.uk/dataset/bin-collection-days"
   :type "dataset"
   :rating "***"}
   {:title "Glasgow City Council - household waste"
    :additional "waste domestic"
    :url "https://data.glasgow.gov.uk/dataset/household-waste"
    :type "dataset"
    :rating "***"}
   {:title "Moray Council - recycling centres"
    :additional "waste tip dump"
    :url "http://www.moray.gov.uk/moray_standard/page_110140.html"
    :type "dataset"
    :rating "***"}
   {:title "North Ayrshire Council - recycling centres"
    :additional "waste tip dump"
    :url "https://maps-north-ayrshire.opendata.arcgis.com/datasets/recycling-centres"
    :type "dataset"
    :rating "***"}
   {:title "Stirling Council - household waste collection"
    :additional "waste domestic bin"
    :url "https://data.stirling.gov.uk/dataset/waste-management"
    :type "dataset"
    :rating "***"}

   {:title "Scottish Government - household waste"
    :additional "waste domestic LoD LD RDF linked cube"
    :url "http://statistics.gov.scot/data/household-waste"
    :type "dataset"
    :rating "*****"}
   {:title "SEPA - portal"
    :additional "waste environmental protection agency"
    :url "https://www.sepa.org.uk/environment/waste/"
    :type "portal"
    :rating "***"}
   {:title "ZWS - The carbon footprint of Scotland's household waste"
    :additional "waste greenhouse gas emission domestion co2"
    :url "https://www.zerowastescotland.org.uk/sites/default/files/2018%20Carbon%20Metric%20HH%20Brief%20-%20Final.pdf"
    :type "article"
    :rating "***"}

   {:title "DCS - Stirling council's household waste collection as linked data cube"
    :additional "waste domestic LoD LD RDF linked cube"
    :url "https://nbviewer.jupyter.org/github/ash-mcc/dcs/blob/df44d254ea7a26840d6621bfbbbd6e47c1072365/stirling-data-experiment/original-data-to-cube.ipynb"
    :type "article"
    :rating "***"}
   {:title "DCS - Whereabouts is recycling improving?"
    :additional "waste domestic household recycling trend council"
    :url "/articles/whereabouts-is-recycling-improving"
    :type "article"
    :rating "**"}])


(defn parse
  "Returns a de-duped list of 2+ letter long words - in lowercase."
  [s]
  (let [l (-> s
            (or "")
            str/lower-case
            (str/split #"\s+")
            distinct)]
    (filter #(>= (count %) 2) l)))

(defn match
  "Returns a list of the 'actual' words that match the 'suggestion' words."
  [suggestion actual]
  (let [suggestion-words (parse suggestion)
        actual-words (parse (str (:title actual) " " (:additional actual)))
        matched-words (for [suggestion-word suggestion-words
                            actual-word actual-words]
                        (when (str/includes? actual-word suggestion-word)
                          actual-word))]
    (->> matched-words
         distinct
         (remove nil?))))


(defn search-indexed-data [suggestion]
      (->> all-indexed-data
           (map #(assoc % :matched (match suggestion %)))
           (sort-by #(count (:matched %)))
           reverse
           (take 10)
           vec))

#?(:clj
   (pc/defresolver indexed-data-resolver [env params]
                   {::pc/output [:autocomplete/indexed-data]}
                   (let [search (get-in env [:ast :params :search])]
                        {:autocomplete/indexed-data (search-indexed-data search)})))

#?(:clj
   (def resolvers [indexed-data-resolver]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CLIENT:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn autocomplete-ident
      "Returns the ident for an autocomplete control. Can be passed a map of props, or a raw ID."
      [id-or-props]
      (if (map? id-or-props)
        [:autocomplete/by-id (:db/id id-or-props)]
        [:autocomplete/by-id id-or-props]))

(defsc CompletionList [this {:keys [values onValueSelect]}]
       {:ident         (fn [] [:component/id ::CompletionList])}
       (dom/ul nil
               (map (fn [{:keys [title url type rating matched]} m]
                        (dom/li {:key url}
                                #_(dom/a {:href "javascript:void(0)" :onClick #(onValueSelect tags)} tags)
                                (if (str/starts-with? url "/")
                                  (dom/a {:onClick (fn [] (rroute/route-to! this example-article/ExampleArticlePage {}))} title)
                                  (dom/a {:href url :target "_blank"} title))
                                (dom/font {:style {:color "#BEBEBE" :fontSize "smaller"}} " (" type ") (" rating ")")
                                (dom/br)
                                (dom/font {:style {:color "#BEBEBE" :fontSize "smaller"}}
                                          "matched on: " (str/join " " matched))))
                    values)))

(def ui-completion-list (comp/factory CompletionList))

(m/defmutation populate-loaded-suggestions
               "Mutation: Autocomplete suggestions are loaded in a non-visible property to prevent flicker. This is
            used as a post mutation to move them to the active UI field so they appear."
               [{:keys [id]}]
               (action [{:keys [state]}]
                       (let [autocomplete-path (autocomplete-ident id)
                             source-path (conj autocomplete-path :autocomplete/loaded-suggestions)
                             target-path (conj autocomplete-path :autocomplete/suggestions)]

                            (swap! state assoc-in target-path (get-in @state source-path)))))


(def get-suggestions
  "A debounced function that will trigger a load of the server suggestions into a temporary locations and fire
a post mutation when that is complete to move them into the main UI view."
  (letfn [(load-suggestions [comp new-value id]
                            #?(:cljs (df/load! comp :autocomplete/indexed-data nil
                                               {:params               {:search new-value}
                                                :marker               false
                                                :post-mutation        `populate-loaded-suggestions
                                                :post-mutation-params {:id id}
                                                :target               (conj (autocomplete-ident id) :autocomplete/loaded-suggestions)}
                                               )))
          ]
    #?(:cls
       load-suggestions
       :cljs
       (gf/debounce load-suggestions 500))))

(defsc Autocomplete [this {:keys [db/id autocomplete/suggestions autocomplete/value] :as props}]
       {:query         [:db/id                             ; the component's ID
                        :autocomplete/loaded-suggestions   ; A place to do the loading, so we can prevent flicker in the UI
                        :autocomplete/suggestions          ; the current completion suggestions
                        :autocomplete/value]               ; the current user-entered value
        :ident         (fn [] (autocomplete-ident props))
        :initial-state (fn [{:keys [id]}] {:db/id id :autocomplete/suggestions [] :autocomplete/value ""})}
       (let [field-id (str "autocomplete-" id)             ; for html label/input association
             ;; server gives us a few, and as the user types we need to filter it further.
             ; _ (pprint (str "suggestions: " (map #(count (:matched %)) suggestions)))
             filtered-suggestions (when (vector? suggestions)
                                        (filter #(> (count (:matched %)) 0) suggestions))
             ; We want to not show the list if they've chosen something valid
             exact-match? (and (= 1 (count filtered-suggestions)) (= value (:matched (first filtered-suggestions))))
             ; When they select an item, we place it's value in the input
             onSelect (fn [v] (m/set-string! this :autocomplete/value :value v))]
            (dom/div {:style {:height "600px"}}
                     (dom/label {:htmlFor field-id} "Search: ")
                     (dom/input {:id       field-id
                                 :value    value
                                 :onChange (fn [evt]
                                               (let [new-value (.. evt -target -value)]
                                                    ; we avoid even looking for help until they've typed a couple of letters
                                                    (if (>= (.-length new-value) 2)
                                                      (get-suggestions this new-value id)
                                                      ; if they shrink the value too much, clear suggestions
                                                      (m/set-value! this :autocomplete/suggestions []))
                                                    ; always update the input itself (controlled)
                                                    (m/set-string! this :autocomplete/value :value new-value)))
                                 :autoComplete "off"})
                     ; show the completion list when it exists and isn't just exactly what they've chosen ...but logic partially bypassed for now
                     (when (and (vector? suggestions) (seq suggestions) #_(not exact-match?))
                           (ui-completion-list {:values filtered-suggestions :onValueSelect onSelect})))))

(def ui-autocomplete (comp/factory Autocomplete))

(defsc AutocompleteRoot [this {:keys [search-input]}]
       {:initial-state (fn [p] {:search-input (comp/get-initial-state Autocomplete {:id :indexed-data})})
        :query         [{:search-input (comp/get-query Autocomplete)}]
        :ident         (fn [] [:component/id ::AutocompleteRoot])
        :route-segment ["search"]}
       (dom/div
         (dom/h3 "Search")
         (dom/p "Search our articles, community comments and index of open data about waste in Scotland.")
         (ui-autocomplete search-input)))