(ns wcs.ui.about
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom]
       :cljs [com.fulcrologic.fulcro.dom :as dom])))

(defsc AboutPage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::AboutPage])
   :initial-state {}
   :route-segment ["about"]}
  (dom/div
    (dom/h3 "About")

    (dom/p "This is the " (dom/b "Waste Commons Scotland") " web application (" (dom/b "WCS") ").")

    (dom/p "The development of WCS is one of the outcomes of the "
           (dom/a {:href "https://www.stir.ac.uk/research/hub/contract/933675" :target "_blank"} "Data Commons Scotland") " research project.")

    (dom/p "The objectives for WCS include:")

    (dom/ul
      (dom/li "Help its user community find, understand and comment on the "
              (dom/i "open data") " about waste management in Scotland.")
      (dom/li "Be a demonstrator of the findings from the encompassing research project, and be an archetype for future portals onto other categories of "
              (dom/i "open data") "."))

    (dom/p (dom/u "WARNING:") " The project is at an early stage and this is mostly " (dom/i "placeholder") " content.")))
