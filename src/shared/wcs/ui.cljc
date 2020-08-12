(ns wcs.ui
  (:require
    #?@(:cljs [[com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown :refer [ui-dropdown]]
               [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown-menu :refer [ui-dropdown-menu]]
               [com.fulcrologic.semantic-ui.modules.dropdown.ui-dropdown-item :refer [ui-dropdown-item]]
               [com.fulcrologic.semantic-ui.collections.menu.ui-menu-item :refer [ui-menu-item]]])
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [wcs.ui.account-forms :refer [AccountForm AccountList]]
    [wcs.ui.invoice-forms :refer [InvoiceForm InvoiceList AccountInvoices]]
    [wcs.ui.item-forms :refer [ItemForm InventoryReport]]
    [wcs.ui.line-item-forms :refer [LineItemForm]]
    [wcs.ui.login-dialog :refer [LoginForm]]
    [wcs.ui.sales-report :as sales-report]
    [wcs.ui.dashboard :as dashboard]
    [wcs.ui.about :as about]
    [wcs.ui.example-article :as example-article]
    [wcs.ui.autocomplete :as autocomplete]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.routing :as rroute]
    [taoensso.timbre :as log]))

(defsc LandingPage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::LandingPage])
   :initial-state {}
   :route-segment ["landing-page"]}
  (dom/div
    (dom/div #_{:style {:background-color "#ccffff"}}
      (dom/h2 "Waste Commons Scotland")
      (dom/font {:style {:color "#E56E94"}} "Search for")
      ", "
      (dom/font {:style {:color "#9CB071"}} " learn about")
      " and"
      (dom/font {:style {:color "#659EC7"}} " comment on")
      " the " (dom/i "open data") " about " (dom/b "waste") " in Scotland.")))


;; This will just be a normal router...but there can be many of them.
(defrouter MainRouter [this {:keys [current-state route-factory route-props]}]
  {:always-render-body? true
   :router-targets      [LandingPage ItemForm InvoiceForm InvoiceList AccountList AccountForm AccountInvoices
                         sales-report/SalesReport InventoryReport
                         sales-report/RealSalesReport
                         dashboard/Dashboard
                         about/AboutPage
                         example-article/ExampleArticlePage
                         autocomplete/AutocompleteRoot]}
  ;; Normal Fulcro code to show a loader on slow route change (assuming Semantic UI here, should
  ;; be generalized for RAD so UI-specific code isn't necessary)
  (dom/div
    (dom/div :.ui.loader {:classes [(when-not (= :routed current-state) "active")]})
    (when route-factory
      (route-factory route-props))))

(def ui-main-router (comp/factory MainRouter))

(auth/defauthenticator Authenticator {:local LoginForm})

(def ui-authenticator (comp/factory Authenticator))

(defsc Root [this {::auth/keys [authorization]
                   ::app/keys  [active-remotes]
                   :keys       [authenticator router]}]
  {:query         [{:authenticator (comp/get-query Authenticator)}
                   {:router (comp/get-query MainRouter)}
                   ::app/active-remotes
                   ::auth/authorization]
   :initial-state {:router        {}
                   :authenticator {}}}
  (let [logged-in? (= :success (some-> authorization :local ::auth/status))
        busy?      (seq active-remotes)
        username   (some-> authorization :local :account/name)]
    (dom/div

         (div :.ui.top.menu
              #?(:cljs
                 (comp/fragment
                   (ui-menu-item {:onClick (fn [] (rroute/route-to! this LandingPage {}))}
                                 (dom/img {:src "/images/dcs.png"}) ent/nbsp "Waste Commons Scotland")
                   (ui-menu-item {:onClick (fn [] (rroute/route-to! this about/AboutPage {}))} "About")
                   (ui-dropdown {:className "item" :text "Articles"}
                                (ui-dropdown-menu {}
                                                  (ui-dropdown-item {:onClick (fn [] (rroute/route-to! this example-article/ExampleArticlePage {}))} "Where is household waste improving?")))

                   (ui-menu-item {:onClick (fn [] (rroute/route-to! this autocomplete/AutocompleteRoot {}))} "Search")))
              (when logged-in?
                #?(:cljs
                   (comp/fragment
                     (ui-dropdown {:className "item" :text "Account"}
                                  (ui-dropdown-menu {}
                                                    (ui-dropdown-item {:onClick (fn [] (rroute/route-to! this AccountList {}))} "View All")
                                                    (ui-dropdown-item {:onClick (fn [] (form/create! this AccountForm))} "New")))
                     (ui-dropdown {:className "item" :text "Inventory"}
                                  (ui-dropdown-menu {}
                                                    (ui-dropdown-item {:onClick (fn [] (rroute/route-to! this InventoryReport {}))} "View All")
                                                    (ui-dropdown-item {:onClick (fn [] (form/create! this ItemForm))} "New")))
                     (ui-dropdown {:className "item" :text "Invoices"}
                                  (ui-dropdown-menu {}
                                                    (ui-dropdown-item {:onClick (fn [] (rroute/route-to! this InvoiceList {}))} "View All")
                                                    (ui-dropdown-item {:onClick (fn [] (form/create! this InvoiceForm))} "New")
                                                    (ui-dropdown-item {:onClick (fn [] (rroute/route-to! this AccountInvoices {:account/id (new-uuid 101)}))} "Invoices for Account 101")))
                     (ui-dropdown {:className "item" :text "Reports"}
                                  (ui-dropdown-menu {}
                                                    (ui-dropdown-item {:onClick (fn [] (rroute/route-to! this dashboard/Dashboard {}))} "Dashboard")
                                                    (ui-dropdown-item {:onClick (fn [] (rroute/route-to! this sales-report/RealSalesReport {}))} "Sales Report"))))))
              (div :.right.menu
                   (div :.item
                        (div :.ui.tiny.loader {:classes [(when busy? "active")]})
                        ent/nbsp ent/nbsp ent/nbsp ent/nbsp)
                   (if logged-in?
                    (comp/fragment
                      (div :.ui.item
                           (str "Logged in as " username))
                      (div :.ui.item
                           (dom/button :.ui.button {:onClick (fn []
                                                               ;; TODO: check if we can change routes...
                                                               (rroute/route-to! this LandingPage {})
                                                               (auth/logout! this :local))}
                                       "Logout")))
                    (div :.ui.item
                         (dom/button :.ui.primary.button {:onClick #(auth/authenticate! this :local nil)}
                                     "Login")))))
      (div :.ui.container.segment
        (ui-authenticator authenticator)
        (ui-main-router router)))))

(def ui-root (comp/factory Root))

