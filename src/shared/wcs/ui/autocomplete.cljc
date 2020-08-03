(ns wcs.ui.autocomplete
  (:require
    #?@(:clj
        [[com.fulcrologic.fulcro.dom-server :as dom]]
        :cljs
        [[com.fulcrologic.fulcro.dom :as dom]
         [goog.functions :as gf]
         [com.fulcrologic.fulcro.data-fetch :as df]])
    [clojure.string :as str]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def airports
  ["London Gatwick"
   "London Heathrow"
   "London Stanstead"
   "Dublin"
   "Glasgow"
   "Belfast Aldergrove"
   "Belfast City"])

(defn airport-search [s]
      (->> airports
           (filter (fn [i] (str/includes? (str/lower-case i) (str/lower-case s))))
           (take 10)
           vec))

#?(:clj
   (pc/defresolver list-resolver [env params]
                   {::pc/output [:autocomplete/airports]}
                   (let [search (get-in env [:ast :params :search])]
                        {:autocomplete/airports (airport-search search)})))

#?(:clj
   (def resolvers [list-resolver]))

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
               (map (fn [v]
                        (dom/li {:key v}
                                (dom/a {:href "javascript:void(0)" :onClick #(onValueSelect v)} v))) values)))

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
                            #?(:cljs (df/load! comp :autocomplete/airports nil
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
                                        (filter #(str/includes? (str/lower-case %) (str/lower-case value)) suggestions))
             ; We want to not show the list if they've chosen something valid
             exact-match? (and (= 1 (count filtered-suggestions)) (= value (first filtered-suggestions)))
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

(defsc AutocompleteRoot [this {:keys [airport-input]}]
       {:initial-state (fn [p] {:airport-input (comp/get-initial-state Autocomplete {:id :airports})})
        :query         [{:airport-input (comp/get-query Autocomplete)}]
        :ident         (fn [] [:component/id ::AutocompleteRoot])
        :route-segment ["search"]}
       (dom/div
         (dom/h3 "Search")
         (dom/p "Search our index of open data about waste in Scotland.")
         (ui-autocomplete airport-input)))