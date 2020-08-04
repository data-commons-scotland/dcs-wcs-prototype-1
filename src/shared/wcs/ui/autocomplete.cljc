(ns wcs.ui.autocomplete
  (:require
    #?@(:clj
        [[com.fulcrologic.fulcro.dom-server :as dom]]
        :cljs
        [[com.fulcrologic.fulcro.dom :as dom]
         [goog.functions :as gf]
         [com.fulcrologic.fulcro.data-fetch :as df]
         #_[com.fulcrologic.semantic-ui.modules.rating.ui-rating :refer [ui-rating]]])
    [clojure.string :as str]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def all-indexed-data
  [{:title "Stirling Council - Household waste collection"
    :tags "stirling household domestic collections"
    :url "https://data.stirling.gov.uk/dataset/waste-management"
    :type "dataset"
    :rating "***"}
   {:title "Dundee City Council - Bin sensor returns"
    :tags "dundee bins sensors"
    :url "https://data.dundeecity.gov.uk/dataset/bin-sensor-returns"
    :type "dataset"
    :rating "***"}
   {:title "Dundee City Council - Recycling points"
    :tags "dundee dumps tips recycling"
    :url "https://data.dundeecity.gov.uk/dataset/recycling-facility-locations"
    :type "dataset"
    :rating "***"}
   {:title "Aberdeenshire Council - Recycling centres"
    :tags "aberdeenshire dumps tips recycling"
    :url "https://www.aberdeenshire.gov.uk/online/open-data/"
    :type "dataset"
    :rating "***"}
   {:title "Stirling's household waste collection as linked data cube"
    :tags "stirling household domestic collections LoD LD RDF linked cube"
    :url "https://nbviewer.jupyter.org/github/ash-mcc/dcs/blob/df44d254ea7a26840d6621bfbbbd6e47c1072365/stirling-data-experiment/original-data-to-cube.ipynb"
    :type "article"
    :rating "***"}])

(defn search-indexed-data [s]
      (->> all-indexed-data
           (filter (fn [i] (str/includes? (str/lower-case (:tags i)) (str/lower-case s))))
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
               (map (fn [{:keys [tags title url type rating]} m]
                        (dom/li {:key tags}
                                #_(dom/a {:href "javascript:void(0)" :onClick #(onValueSelect tags)} tags)
                                (dom/a {:href url :target "_blank"} title)
                                (dom/br)
                                (dom/font {:style {:color "#BEBEBE" :font-size "smaller"}}
                                          "tags: " tags " | type: " type " | rating: " rating)))
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
         #_(gf/debounce load-suggestions 500)
         load-suggestions))

(defsc Autocomplete [this {:keys [db/id autocomplete/suggestions autocomplete/value] :as props}]
       {:query         [:db/id                             ; the component's ID
                        :autocomplete/loaded-suggestions   ; A place to do the loading, so we can prevent flicker in the UI
                        :autocomplete/suggestions          ; the current completion suggestions
                        :autocomplete/value]               ; the current user-entered value
        :ident         (fn [] (autocomplete-ident props))
        :initial-state (fn [{:keys [id]}] {:db/id id :autocomplete/suggestions [] :autocomplete/value ""})}
       (let [field-id (str "autocomplete-" id)             ; for html label/input association
             ;; server gives us a few, and as the user types we need to filter it further.
             filtered-suggestions (when (vector? suggestions)
                                        (filter #(str/includes? (str/lower-case (:tags %)) (str/lower-case value)) suggestions))
             ; We want to not show the list if they've chosen something valid
             exact-match? (and (= 1 (count filtered-suggestions)) (= value (:tags (first filtered-suggestions))))
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
                                                    (m/set-string! this :autocomplete/value :value new-value)))})
                     ; show the completion list when it exists and isn't just exactly what they've chosen
                     (when (and (vector? suggestions) (seq suggestions) (not exact-match?))
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